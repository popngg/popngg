-- Execute in the same MySQL session as 05_stage_ambiguous_medals.sql.
-- Replace __TARGET_DB__ and __EXPECTED_READY__ only after a full backup and
-- a successful rehearsal against a separate copy of the target database.
-- The legacy collector encoded both medal=12 and rank=9 as image `none`.
-- This operation follows the product decision to display eligible old medals
-- as BLACK_CIRCLE(10), while keeping their unobserved rank as NO_RANK(13).
SET SESSION sql_mode = 'STRICT_ALL_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

CREATE TEMPORARY TABLE code12_restore_ready AS
SELECT new_playdata_id, user_id, chart_id, source_score
  FROM code12_audit
 WHERE audit_status = 'REVIEW'
   AND source_rank = 9 AND current_rank = 9 AND source_score > 0;
CREATE TEMPORARY TABLE code12_restore_users AS
SELECT DISTINCT user_id FROM code12_restore_ready;
CREATE TEMPORARY TABLE code12_restore_assertion (ok INT NOT NULL);
INSERT INTO code12_restore_assertion (ok)
SELECT IF(COUNT(*) = __EXPECTED_READY__, 1, NULL)
  FROM code12_restore_ready;

START TRANSACTION;
-- The renewal path locks users before playdata. Lock the same rows first.
UPDATE `__TARGET_DB__`.users u
JOIN code12_restore_users affected ON affected.user_id = u.user_id
   SET u.updated_at = u.updated_at;

UPDATE `__TARGET_DB__`.playdata p
JOIN code12_restore_ready ready ON ready.new_playdata_id = p.playdata_id
JOIN `__TARGET_DB__`.users u ON u.user_id = p.user_id
   SET p.medal_code = 10,
       p.all_time_rank_code = 13,
       p.updated_at = CURRENT_TIMESTAMP
 WHERE p.user_id = ready.user_id
   AND p.chart_id = ready.chart_id
   AND p.all_time_score = ready.source_score
   AND p.medal_code = 12
   AND p.all_time_rank_code = 9
   AND p.current_version = 29
   AND p.all_time_score_version = 28
   AND p.version_score_known = FALSE
   AND p.last_renew_log_id IS NULL
   AND NOT EXISTS (
       SELECT 1 FROM `__TARGET_DB__`.renew_logs renewed
        WHERE renewed.created_at >= '2026-07-24 00:00:00'
          AND (renewed.user_id = p.user_id OR renewed.poptomo_id = u.poptomo_id)
   );
SET @restored_count = ROW_COUNT();
INSERT INTO code12_restore_assertion (ok)
SELECT IF(@restored_count = __EXPECTED_READY__, 1, NULL);

-- Match the application's potential-popclass formula for every affected user.
UPDATE `__TARGET_DB__`.playdata p
JOIN code12_restore_users affected ON affected.user_id = p.user_id
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

CREATE TEMPORARY TABLE code12_restore_points AS
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
  JOIN code12_restore_users affected ON affected.user_id = p.user_id
  JOIN `__TARGET_DB__`.charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE;

CREATE TEMPORARY TABLE code12_restore_ranked AS
SELECT points.*,
       CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END AS bucket,
       ROW_NUMBER() OVER (
         PARTITION BY user_id,
           CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END
         ORDER BY potential_popclass DESC, all_time_score DESC, chart_id
       ) AS bucket_rank
  FROM code12_restore_points points;
CREATE TEMPORARY TABLE code12_restore_totals AS
SELECT user_id, FLOOR(SUM(point_hundredths) / 60) * 10 AS potential_popclass
  FROM code12_restore_ranked
 WHERE (bucket = 'CURRENT' AND bucket_rank <= 20)
    OR (bucket = 'OLD' AND bucket_rank <= 40)
 GROUP BY user_id;

UPDATE `__TARGET_DB__`.user_profiles profile
JOIN code12_restore_users affected ON affected.user_id = profile.user_id
LEFT JOIN code12_restore_totals totals ON totals.user_id = profile.user_id
   SET profile.potential_popclass = COALESCE(totals.potential_popclass, 0),
       profile.updated_at = CURRENT_TIMESTAMP;

DELETE levels FROM `__TARGET_DB__`.user_clear_levels levels
JOIN code12_restore_users affected ON affected.user_id = levels.user_id;
INSERT INTO `__TARGET_DB__`.user_clear_levels (user_id, current_version, clear_level)
SELECT p.user_id, p.current_version, MAX(c.level)
  FROM `__TARGET_DB__`.playdata p
  JOIN code12_restore_users affected ON affected.user_id = p.user_id
  JOIN `__TARGET_DB__`.charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE AND p.medal_code IN (1,2,3,4,5,6,7,11,12)
 GROUP BY p.user_id, p.current_version;
UPDATE `__TARGET_DB__`.user_directory_revision
   SET revision = revision + 1 WHERE id = 1;
COMMIT;

SELECT 'restored' AS result, @restored_count AS record_count,
       (SELECT COUNT(*) FROM code12_restore_users) AS user_count;
