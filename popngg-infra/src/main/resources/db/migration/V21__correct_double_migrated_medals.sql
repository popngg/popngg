-- V16 was applied to rows whose legacy medal codes had already been converted
-- once. The restored source dump audit confirmed a second conversion on all
-- 53,433 unrenewed rows with legacy codes 8..11. Reverse exactly one pass.
CREATE TEMPORARY TABLE double_migrated_medal_users AS
SELECT DISTINCT user_id
  FROM playdata
 WHERE current_version = 29
   AND all_time_score_version = 28
   AND version_score_known = FALSE
   AND last_renew_log_id IS NULL
   AND medal_code IN (8, 9, 10, 11);

UPDATE playdata
   SET medal_code = CASE medal_code
       WHEN 8 THEN 9
       WHEN 9 THEN 10
       WHEN 10 THEN 11
       WHEN 11 THEN 8
       ELSE medal_code
   END
 WHERE current_version = 29
   AND all_time_score_version = 28
   AND version_score_known = FALSE
   AND last_renew_log_id IS NULL
   AND medal_code IN (8, 9, 10, 11);

UPDATE playdata p
JOIN double_migrated_medal_users affected ON affected.user_id = p.user_id
JOIN charts c ON c.chart_id = p.chart_id
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

CREATE TEMPORARY TABLE corrected_medal_points AS
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
  FROM playdata p
  JOIN double_migrated_medal_users affected ON affected.user_id = p.user_id
  JOIN charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE;

CREATE TEMPORARY TABLE corrected_medal_ranked AS
SELECT points.*,
       CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END AS bucket,
       ROW_NUMBER() OVER (
         PARTITION BY user_id,
           CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END
         ORDER BY potential_popclass DESC, all_time_score DESC, chart_id
       ) AS bucket_rank
  FROM corrected_medal_points points;

CREATE TEMPORARY TABLE corrected_medal_totals AS
SELECT user_id, FLOOR(SUM(point_hundredths) / 60) * 10 AS potential_popclass
  FROM corrected_medal_ranked
 WHERE (bucket = 'CURRENT' AND bucket_rank <= 20)
    OR (bucket = 'OLD' AND bucket_rank <= 40)
 GROUP BY user_id;

UPDATE user_profiles profile
JOIN double_migrated_medal_users affected ON affected.user_id = profile.user_id
LEFT JOIN corrected_medal_totals totals ON totals.user_id = profile.user_id
   SET profile.potential_popclass = COALESCE(totals.potential_popclass, 0);

DROP TEMPORARY TABLE corrected_medal_totals;
DROP TEMPORARY TABLE corrected_medal_ranked;
DROP TEMPORARY TABLE corrected_medal_points;
DROP TEMPORARY TABLE double_migrated_medal_users;
