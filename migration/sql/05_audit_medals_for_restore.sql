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
