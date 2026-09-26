SELECT audit_status, COUNT(*) AS record_count,
       COUNT(DISTINCT user_id) AS user_count,
       SUM(source_score = 0) AS zero_score_count,
       SUM(source_score > 0) AS positive_score_count
  FROM code12_audit
 GROUP BY audit_status ORDER BY audit_status;

SELECT 'summary' AS section,
       (SELECT COUNT(*) FROM `__LEGACY_DB__`.playdata WHERE medal = 12) AS source_code12_rows,
       COUNT(*) AS mapped_rows,
       (SELECT COUNT(*) FROM `__LEGACY_DB__`.playdata WHERE medal = 12) - COUNT(*) AS unmapped_rows,
       SUM(audit_status = 'REVIEW') AS review_rows
  FROM code12_audit;

SELECT COALESCE(CAST(h.last_history_medal AS CHAR), 'NO_HISTORY') AS latest_history_medal,
       COUNT(*) AS review_rows,
       COALESCE(SUM(h.last_history_score = a.source_score), 0) AS matching_score_rows
  FROM code12_review_history h
  JOIN code12_audit a ON a.old_playdata_id = h.old_playdata_id
 GROUP BY h.last_history_medal ORDER BY review_rows DESC;
