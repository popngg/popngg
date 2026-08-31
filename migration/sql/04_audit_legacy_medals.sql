-- Compare the restored legacy dump with rows that have never been renewed from
-- the pop'n 29 source. Output is aggregate-only and contains no user IDs.
SELECT 'summary' AS section,
       COUNT(*) AS eligible_rows,
       SUM(current.medal_code <> CASE legacy.medal
           WHEN 0 THEN 13 WHEN 8 THEN 11 WHEN 9 THEN 8
           WHEN 10 THEN 9 WHEN 11 THEN 10 ELSE legacy.medal END) AS mismatch_rows
  FROM `__LEGACY_DB__`.playdata legacy
  JOIN `__TARGET_DB__`.migration_playdata_map map
    ON map.old_playdata_id = legacy.playdata_id
  JOIN `__TARGET_DB__`.playdata current
    ON current.playdata_id = map.new_playdata_id
 WHERE current.all_time_score_version = 28
   AND current.version_score_known = FALSE
   AND current.last_renew_log_id IS NULL;

SELECT 'mismatch_by_code' AS section,
       legacy.medal AS legacy_code,
       CASE legacy.medal
           WHEN 0 THEN 13 WHEN 8 THEN 11 WHEN 9 THEN 8
           WHEN 10 THEN 9 WHEN 11 THEN 10 ELSE legacy.medal
       END AS expected_code,
       current.medal_code AS actual_code,
       COUNT(*) AS row_count
  FROM `__LEGACY_DB__`.playdata legacy
  JOIN `__TARGET_DB__`.migration_playdata_map map
    ON map.old_playdata_id = legacy.playdata_id
  JOIN `__TARGET_DB__`.playdata current
    ON current.playdata_id = map.new_playdata_id
 WHERE current.all_time_score_version = 28
   AND current.version_score_known = FALSE
   AND current.last_renew_log_id IS NULL
   AND current.medal_code <> CASE legacy.medal
       WHEN 0 THEN 13 WHEN 8 THEN 11 WHEN 9 THEN 8
       WHEN 10 THEN 9 WHEN 11 THEN 10 ELSE legacy.medal END
 GROUP BY legacy.medal, expected_code, current.medal_code
 ORDER BY legacy.medal, current.medal_code;
