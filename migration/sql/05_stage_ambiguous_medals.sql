-- Only temporary tables are created. Source and target records are never updated.
-- 2026-08-30 00:00 KST is 2026-08-29 15:00 UTC.
CREATE TEMPORARY TABLE code12_renewed_since_dump (user_id BIGINT PRIMARY KEY);
INSERT IGNORE INTO code12_renewed_since_dump
SELECT user_id FROM `__TARGET_DB__`.renew_logs
 WHERE user_id IS NOT NULL AND created_at >= '2026-07-24 00:00:00';
INSERT IGNORE INTO code12_renewed_since_dump
SELECT u.user_id FROM `__TARGET_DB__`.renew_logs r
JOIN `__TARGET_DB__`.users u ON u.poptomo_id = r.poptomo_id
 WHERE r.created_at >= '2026-07-24 00:00:00';

CREATE TEMPORARY TABLE code12_renewed_after_cutoff (user_id BIGINT PRIMARY KEY);
INSERT IGNORE INTO code12_renewed_after_cutoff
SELECT user_id FROM `__TARGET_DB__`.renew_logs
 WHERE user_id IS NOT NULL AND created_at >= '2026-08-29 15:00:00';
INSERT IGNORE INTO code12_renewed_after_cutoff
SELECT u.user_id FROM `__TARGET_DB__`.renew_logs r
JOIN `__TARGET_DB__`.users u ON u.poptomo_id = r.poptomo_id
 WHERE r.created_at >= '2026-08-29 15:00:00';

CREATE TEMPORARY TABLE code12_audit AS
SELECT legacy.playdata_id AS old_playdata_id,
       current.playdata_id AS new_playdata_id,
       current.user_id, current.chart_id,
       legacy.score AS source_score,
       current.medal_code AS current_medal,
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
           WHEN current.medal_code <> 12 THEN 'CURRENT_MEDAL_CHANGED'
           ELSE 'REVIEW'
       END AS audit_status
  FROM `__LEGACY_DB__`.playdata legacy
  JOIN `__TARGET_DB__`.migration_playdata_map mapped
    ON mapped.old_playdata_id = legacy.playdata_id
  JOIN `__TARGET_DB__`.playdata current
    ON current.playdata_id = mapped.new_playdata_id
  LEFT JOIN code12_renewed_since_dump renewed ON renewed.user_id = current.user_id
  LEFT JOIN code12_renewed_after_cutoff cutoff ON cutoff.user_id = current.user_id
 WHERE legacy.medal = 12;

CREATE TEMPORARY TABLE code12_review_history AS
SELECT a.old_playdata_id,
       (SELECT h.medal FROM `__LEGACY_DB__`.history h
         WHERE h.user_id = a.user_id AND h.chart_id = a.chart_id
         ORDER BY h.history_id DESC LIMIT 1) AS last_history_medal,
       (SELECT h.score FROM `__LEGACY_DB__`.history h
         WHERE h.user_id = a.user_id AND h.chart_id = a.chart_id
         ORDER BY h.history_id DESC LIMIT 1) AS last_history_score
  FROM code12_audit a WHERE a.audit_status = 'REVIEW';
