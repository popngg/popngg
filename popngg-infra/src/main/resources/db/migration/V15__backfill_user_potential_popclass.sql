-- V14 cached every chart's potential value, but migrated profiles still have
-- potential_popclass=0 because they have not run a new renewal. Rebuild the
-- official user total from all-time scores: current 20 + old 40.
CREATE TEMPORARY TABLE potential_chart_points AS
SELECT p.user_id,
       p.chart_id,
       c.chart_version,
       p.all_time_score,
       p.potential_popclass,
       CASE
           WHEN p.all_time_score < 50000 THEN 0
           ELSE FLOOR(
               FLOOR(
                   c.level * (
                       3750 * c.level
                       + CASE
                           WHEN p.medal_code = 1 THEN 21250
                           WHEN p.medal_code IN (2, 3, 4) THEN 17500
                           WHEN p.medal_code IN (5, 6, 7) THEN 12500
                           WHEN p.medal_code = 11 THEN 6250
                           WHEN p.medal_code = 12 THEN 10000
                           ELSE 0
                         END
                       + p.all_time_score - 50000
                   ) * 100000000 / 3881250
               ) * 6000 / 100000000
           )
       END AS point_hundredths
  FROM playdata p
  JOIN charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE;

CREATE TEMPORARY TABLE potential_ranked AS
SELECT points.*,
       CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END AS bucket,
       ROW_NUMBER() OVER (
           PARTITION BY user_id,
                        CASE WHEN chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END
           ORDER BY potential_popclass DESC, all_time_score DESC, chart_id
       ) AS bucket_rank
  FROM potential_chart_points points;

CREATE TEMPORARY TABLE potential_user_totals AS
SELECT user_id,
       FLOOR(SUM(point_hundredths) / 60) * 10 AS potential_popclass
  FROM potential_ranked
 WHERE (bucket = 'CURRENT' AND bucket_rank <= 20)
    OR (bucket = 'OLD' AND bucket_rank <= 40)
 GROUP BY user_id;

UPDATE user_profiles profile
LEFT JOIN potential_user_totals totals ON totals.user_id = profile.user_id
   SET profile.potential_popclass = COALESCE(totals.potential_popclass, 0);

DROP TEMPORARY TABLE potential_user_totals;
DROP TEMPORARY TABLE potential_ranked;
DROP TEMPORARY TABLE potential_chart_points;
