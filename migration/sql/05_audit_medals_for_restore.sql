-- Read-only audit. These temporary tables disappear with the mysql session.
-- The source dump was captured on 2026-07-24. The earlier UTC bound is
-- deliberately conservative when the exact capture time is uncertain.
CREATE TEMPORARY TABLE protected_since_dump (user_id BIGINT PRIMARY KEY);
INSERT IGNORE INTO protected_since_dump
SELECT user_id FROM `__TARGET_DB__`.renew_logs
 WHERE user_id IS NOT NULL AND created_at >= '2026-07-24 00:00:00';
INSERT IGNORE INTO protected_since_dump
SELECT u.user_id FROM `__TARGET_DB__`.renew_logs r
JOIN `__TARGET_DB__`.users u ON u.poptomo_id = r.poptomo_id
 WHERE r.created_at >= '2026-07-24 00:00:00';

-- 2026-08-30 00:00 KST = 2026-08-29 15:00 UTC. Report this separately
-- because these users must never be changed, even if a chart row looks old.
CREATE TEMPORARY TABLE protected_after_cutoff (user_id BIGINT PRIMARY KEY);
INSERT IGNORE INTO protected_after_cutoff
SELECT user_id FROM `__TARGET_DB__`.renew_logs
 WHERE user_id IS NOT NULL AND created_at >= '2026-08-29 15:00:00';
INSERT IGNORE INTO protected_after_cutoff
SELECT u.user_id FROM `__TARGET_DB__`.renew_logs r
JOIN `__TARGET_DB__`.users u ON u.poptomo_id = r.poptomo_id
 WHERE r.created_at >= '2026-08-29 15:00:00';

CREATE TEMPORARY TABLE medal_restore_audit AS
SELECT legacy.playdata_id AS old_playdata_id,
       current.playdata_id AS new_playdata_id,
       current.user_id,
       legacy.medal AS old_medal,
       current.medal_code AS current_medal,
       CASE legacy.medal
           WHEN 8 THEN 11 WHEN 9 THEN 8 WHEN 10 THEN 9
           WHEN 11 THEN 10 WHEN 12 THEN 13
           ELSE legacy.medal END AS expected_medal,
       CASE
           WHEN legacy.user_id <> current.user_id
             OR legacy.chart_id <> current.chart_id THEN 'IDENTITY_MISMATCH'
           WHEN cutoff.user_id IS NOT NULL THEN 'RENEWED_AFTER_AUG30'
           WHEN renewed.user_id IS NOT NULL THEN 'RENEWED_SINCE_DUMP'
           WHEN current.last_renew_log_id IS NOT NULL THEN 'RENEWED_ROW'
           WHEN legacy.score <> current.all_time_score THEN 'SCORE_CHANGED'
           WHEN current.current_version <> 29
             OR current.all_time_score_version <> 28
             OR current.version_score_known <> FALSE THEN 'STATE_CHANGED'
           WHEN legacy.medal = 12 THEN 'SOURCE_NONE_REVIEW'
           WHEN legacy.medal NOT BETWEEN 1 AND 11 THEN 'UNKNOWN_SOURCE_CODE'
           WHEN current.medal_code = CASE legacy.medal
               WHEN 8 THEN 11 WHEN 9 THEN 8 WHEN 10 THEN 9
               WHEN 11 THEN 10 ELSE legacy.medal END THEN 'ALREADY_CORRECT'
           ELSE 'READY'
       END AS audit_status
  FROM `__LEGACY_DB__`.playdata legacy
  JOIN `__TARGET_DB__`.migration_playdata_map mapped
    ON mapped.old_playdata_id = legacy.playdata_id
  JOIN `__TARGET_DB__`.playdata current
    ON current.playdata_id = mapped.new_playdata_id
  LEFT JOIN protected_since_dump renewed ON renewed.user_id = current.user_id
  LEFT JOIN protected_after_cutoff cutoff ON cutoff.user_id = current.user_id;

SELECT audit_status, old_medal, expected_medal, current_medal,
       COUNT(*) AS record_count, COUNT(DISTINCT user_id) AS user_count
  FROM medal_restore_audit
 GROUP BY audit_status, old_medal, expected_medal, current_medal
 ORDER BY audit_status, old_medal, current_medal;

SELECT 'summary' AS section,
       COUNT(*) AS mapped_rows,
       SUM(audit_status = 'READY') AS ready_rows,
       SUM(audit_status = 'SOURCE_NONE_REVIEW') AS source_none_rows,
       SUM(audit_status = 'RENEWED_AFTER_AUG30') AS protected_rows
  FROM medal_restore_audit;

DROP TEMPORARY TABLE medal_restore_audit;
DROP TEMPORARY TABLE protected_after_cutoff;
DROP TEMPORARY TABLE protected_since_dump;
