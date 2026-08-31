-- The legacy dump used 8=EASY and 9..11 for the three failed medals.
-- V7 ran before that dump was merged, so the imported values missed its
-- conversion. V12 later promoted those rows to version 29 and marked their
-- version-best score as unknown. That marker lets us avoid records renewed
-- from the actual version 29 source.
CREATE TEMPORARY TABLE legacy_medal_users AS
SELECT DISTINCT user_id
  FROM playdata
 WHERE current_version = 29
   AND version_score_known = FALSE
   AND last_renew_log_id IS NULL;

UPDATE playdata
   SET medal_code = CASE medal_code
       WHEN 8 THEN 11
       WHEN 9 THEN 8
       WHEN 10 THEN 9
       WHEN 11 THEN 10
       WHEN 0 THEN 13
       ELSE medal_code
   END,
       updated_at = CURRENT_TIMESTAMP
 WHERE current_version = 29
   AND version_score_known = FALSE
   AND last_renew_log_id IS NULL
   AND medal_code IN (0, 8, 9, 10, 11);

-- Keep the cached all-time chart value consistent with the corrected medal.
UPDATE playdata p
JOIN legacy_medal_users affected ON affected.user_id = p.user_id
JOIN charts c ON c.chart_id = p.chart_id
SET p.potential_popclass = CASE
    WHEN p.all_time_score < 50000 OR c.is_deleted = TRUE THEN 0
    ELSE FLOOR(
        FLOOR(
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
        ) * 1000 / 6000
    )
END;

CREATE TEMPORARY TABLE corrected_potential_ranked AS
SELECT p.user_id,
       p.chart_id,
       p.all_time_score,
       p.potential_popclass,
       CASE WHEN c.chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END AS bucket,
       ROW_NUMBER() OVER (
           PARTITION BY p.user_id,
                        CASE WHEN c.chart_version = 29 THEN 'CURRENT' ELSE 'OLD' END
           ORDER BY p.potential_popclass DESC, p.all_time_score DESC, p.chart_id
       ) AS bucket_rank,
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
  JOIN legacy_medal_users affected ON affected.user_id = p.user_id
  JOIN charts c ON c.chart_id = p.chart_id
 WHERE c.is_deleted = FALSE;

CREATE TEMPORARY TABLE corrected_potential_totals AS
SELECT user_id,
       FLOOR(SUM(point_hundredths) / 60) * 10 AS potential_popclass
  FROM corrected_potential_ranked
 WHERE (bucket = 'CURRENT' AND bucket_rank <= 20)
    OR (bucket = 'OLD' AND bucket_rank <= 40)
 GROUP BY user_id;

UPDATE user_profiles profile
JOIN legacy_medal_users affected ON affected.user_id = profile.user_id
LEFT JOIN corrected_potential_totals totals ON totals.user_id = profile.user_id
   SET profile.potential_popclass = COALESCE(totals.potential_popclass, 0),
       profile.updated_at = CURRENT_TIMESTAMP;

-- Imported history rows still carry the legacy code table as well.
UPDATE playdata_history
   SET previous_medal_code = CASE previous_medal_code
           WHEN 8 THEN 11 WHEN 9 THEN 8 WHEN 10 THEN 9 WHEN 11 THEN 10
           WHEN 0 THEN 13 ELSE previous_medal_code
       END,
       medal_code = CASE medal_code
           WHEN 8 THEN 11 WHEN 9 THEN 8 WHEN 10 THEN 9 WHEN 11 THEN 10
           WHEN 0 THEN 13 ELSE medal_code
       END
 WHERE game_version = 28
   AND event_type = 'MIGRATION'
   AND (previous_medal_code IN (0, 8, 9, 10, 11)
        OR medal_code IN (0, 8, 9, 10, 11));

DROP TEMPORARY TABLE corrected_potential_totals;
DROP TEMPORARY TABLE corrected_potential_ranked;
DROP TEMPORARY TABLE legacy_medal_users;
