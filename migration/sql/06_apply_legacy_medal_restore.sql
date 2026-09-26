-- Run only after the read-only audit and an encrypted database backup.
-- The preceding staging SQL leaves medal_restore_audit in this session.
SET SESSION sql_mode = 'STRICT_ALL_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
CREATE TEMPORARY TABLE medal_restore_ready AS
SELECT new_playdata_id, user_id, current_medal, expected_medal
  FROM medal_restore_audit WHERE audit_status = 'READY';
CREATE TEMPORARY TABLE medal_restore_users AS
SELECT DISTINCT user_id FROM medal_restore_ready;
CREATE TEMPORARY TABLE medal_restore_assertion (ok INT NOT NULL);
INSERT INTO medal_restore_assertion (ok)
SELECT IF(COUNT(*) = __EXPECTED_READY__, 1, NULL)
  FROM medal_restore_ready;

START TRANSACTION;
-- Imports lock the users row before writing any playdata. Take the same lock
-- so a user cannot renew while their old records are being restored.
UPDATE `__TARGET_DB__`.users u
JOIN medal_restore_users affected ON affected.user_id = u.user_id
   SET u.updated_at = u.updated_at;

UPDATE `__TARGET_DB__`.playdata p
JOIN medal_restore_ready ready ON ready.new_playdata_id = p.playdata_id
JOIN `__TARGET_DB__`.users u ON u.user_id = p.user_id
   SET p.medal_code = ready.expected_medal,
       p.updated_at = CURRENT_TIMESTAMP
 WHERE p.user_id = ready.user_id
   AND p.medal_code = ready.current_medal
   AND p.last_renew_log_id IS NULL
   AND NOT EXISTS (
       SELECT 1 FROM `__TARGET_DB__`.renew_logs renewed
        WHERE renewed.created_at >= '2026-07-24 00:00:00'
          AND (renewed.user_id = p.user_id OR renewed.poptomo_id = u.poptomo_id)
   );
SET @restored_count = ROW_COUNT();
-- A changed candidate set must roll back the transaction rather than commit
-- a partial repair. With strict SQL mode, inserting NULL here is an error.
INSERT INTO medal_restore_assertion (ok)
SELECT IF(@restored_count = __EXPECTED_READY__, 1, NULL);

UPDATE `__TARGET_DB__`.playdata p
JOIN medal_restore_users affected ON affected.user_id = p.user_id
JOIN `__TARGET_DB__`.charts c ON c.chart_id = p.chart_id
SET p.potential_popclass = CASE
    WHEN p.all_time_score < 50000 OR c.is_deleted = TRUE THEN 0
    ELSE FLOOR(FLOOR(FLOOR(c.level * (
        3750 * c.level
        + CASE
            WHEN p.medal_code = 1 THEN 21250
            WHEN p.medal_code IN (2, 3, 4) THEN 17500
            WHEN p.medal_code IN (5, 6, 7) THEN 12500
            WHEN p.medal_code = 11 THEN 6250
            WHEN p.medal_code = 12 THEN 10000
            ELSE 0 END
        + p.all_time_score - 50000
    ) * 100000000 / 3881250) * 6000 / 100000000) * 1000 / 6000)
END;

CREATE TEMPORARY TABLE medal_restore_points AS
SELECT p.user_id, p.chart_id, c.chart_version, p.all_time_score,
       p.potential_popclass,
       CASE WHEN p.all_time_score < 50000 THEN 0 ELSE
         FLOOR(FLOOR(c.level * (3750 * c.level
           + CASE
               WHEN p.medal_code = 1 THEN 21250
               WHEN p.medal_code IN (2, 3, 4) THEN 17500
               WHEN p.medal_code IN (5, 6, 7) THEN 12500
               WHEN p.medal_code = 11 THEN 6250
               WHEN p.medal_code = 12 THEN 10000
               ELSE 0 END
           + p.all_time_score - 50000) * 100000000 / 3881250)
           * 6000 / 100000000) END AS point_hundredths
  FROM `__TARGET_DB__`.playdata p
  JOIN medal_restore_users affected ON affected.user_id = p.user_id
  JOIN `__TARGET_DB__`.charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE;

CREATE TEMPORARY TABLE medal_restore_ranked AS
SELECT points.*,
       CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END AS bucket,
       ROW_NUMBER() OVER (
         PARTITION BY user_id,
           CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END
         ORDER BY potential_popclass DESC, all_time_score DESC, chart_id
       ) AS bucket_rank
  FROM medal_restore_points points;
CREATE TEMPORARY TABLE medal_restore_totals AS
SELECT user_id, FLOOR(SUM(point_hundredths) / 60) * 10 AS potential_popclass
  FROM medal_restore_ranked
 WHERE (bucket = 'CURRENT' AND bucket_rank <= 20)
    OR (bucket = 'OLD' AND bucket_rank <= 40)
 GROUP BY user_id;

UPDATE `__TARGET_DB__`.user_profiles profile
JOIN medal_restore_users affected ON affected.user_id = profile.user_id
LEFT JOIN medal_restore_totals totals ON totals.user_id = profile.user_id
   SET profile.potential_popclass = COALESCE(totals.potential_popclass, 0);

DELETE levels FROM `__TARGET_DB__`.user_clear_levels levels
JOIN medal_restore_users affected ON affected.user_id = levels.user_id;
INSERT INTO `__TARGET_DB__`.user_clear_levels (user_id, current_version, clear_level)
SELECT p.user_id, p.current_version, MAX(c.level)
  FROM `__TARGET_DB__`.playdata p
  JOIN medal_restore_users affected ON affected.user_id = p.user_id
  JOIN `__TARGET_DB__`.charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE AND p.medal_code IN (1,2,3,4,5,6,7,11,12)
 GROUP BY p.user_id, p.current_version;
UPDATE `__TARGET_DB__`.user_directory_revision
   SET revision = revision + 1 WHERE id = 1;
COMMIT;

SELECT 'restored' AS result, @restored_count AS record_count;
