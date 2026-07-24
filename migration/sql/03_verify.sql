USE `__TARGET_DB__`;
TRUNCATE TABLE migration_verification_results;

INSERT INTO migration_verification_results
SELECT 'users_source_vs_target',
       ABS((SELECT COUNT(*) FROM `__LEGACY_DB__`.`user` WHERE poptomo_id IS NOT NULL AND poptomo_id <> '' AND password IS NOT NULL)
         - (SELECT COUNT(*) FROM users)), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT 'users_profiles_one_to_one',
       (SELECT COUNT(*) FROM users u LEFT JOIN user_profiles p ON p.user_id = u.user_id WHERE p.user_id IS NULL)
     + (SELECT COUNT(*) FROM user_profiles p LEFT JOIN users u ON u.user_id = p.user_id WHERE u.user_id IS NULL),
       CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT 'charts_source_vs_target_and_failures',
       ABS((SELECT COUNT(*) FROM `__LEGACY_DB__`.chart)
         - (SELECT COUNT(*) FROM charts)
         - (SELECT COUNT(*) FROM migration_failures WHERE source_table = 'chart')),
       CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT 'songs_have_charts',
       COUNT(*), CURRENT_TIMESTAMP FROM songs s LEFT JOIN charts c ON c.song_id = s.song_id
       WHERE c.chart_id IS NULL;

INSERT INTO migration_verification_results
SELECT 'playdata_source_accounted',
       ABS((SELECT COUNT(*) FROM `__LEGACY_DB__`.playdata)
         - (SELECT COUNT(*) FROM playdata)
         - (SELECT COUNT(*) FROM migration_failures WHERE source_table = 'playdata')),
       CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT 'playdata_orphan_user',
       COUNT(*), CURRENT_TIMESTAMP FROM playdata p LEFT JOIN users u ON u.user_id = p.user_id
       WHERE u.user_id IS NULL;

INSERT INTO migration_verification_results
SELECT 'playdata_orphan_chart',
       COUNT(*), CURRENT_TIMESTAMP FROM playdata p LEFT JOIN charts c ON c.chart_id = p.chart_id
       WHERE c.chart_id IS NULL;

INSERT INTO migration_verification_results
SELECT 'playdata_user_chart_duplicates', COUNT(*), CURRENT_TIMESTAMP
FROM (
  SELECT user_id, chart_id FROM playdata GROUP BY user_id, chart_id HAVING COUNT(*) > 1
) duplicates;

INSERT INTO migration_verification_results
SELECT 'playdata_version_28_policy',
       COUNT(*), CURRENT_TIMESTAMP FROM playdata
       WHERE current_version <> 28 OR version_score <> all_time_score
          OR all_time_score_version <> 28 OR version_rank_code <> all_time_rank_code;

INSERT INTO migration_verification_results
SELECT 'legacy_popclass_preserved',
       COUNT(*), CURRENT_TIMESTAMP
FROM user_profiles p
JOIN migration_user_map m ON m.new_user_id = p.user_id
JOIN `__LEGACY_DB__`.`user` u ON u.user_id = m.old_user_id
WHERE p.legacy_popclass <> COALESCE(u.popclass, 0);

INSERT INTO migration_verification_results
SELECT 'potential_popclass_recalculated',
       COUNT(*), CURRENT_TIMESTAMP
FROM user_profiles p
LEFT JOIN (
  SELECT user_id,
         FLOOR(SUM(CASE WHEN score_rank <= 50 THEN popclass ELSE 0 END) / 50) AS expected
  FROM (
    SELECT user_id, popclass,
           ROW_NUMBER() OVER (
             PARTITION BY user_id ORDER BY all_time_score DESC, popclass DESC, playdata_id
           ) AS score_rank
    FROM playdata
  ) ranked
  GROUP BY user_id
) expected ON expected.user_id = p.user_id
WHERE p.potential_popclass <> COALESCE(expected.expected, 0);

INSERT INTO migration_verification_results
SELECT 'credits_initialized_to_zero',
       COUNT(*), CURRENT_TIMESTAMP FROM user_profiles
       WHERE normal_credit <> 0 OR extra_credit <> 0
          OR time_play_10_credit <> 0 OR time_play_16_credit <> 0;

INSERT INTO migration_verification_results
SELECT 'mapping_coverage',
       (SELECT COUNT(*) FROM users) - (SELECT COUNT(*) FROM migration_user_map)
     + (SELECT COUNT(*) FROM charts) - (SELECT COUNT(*) FROM migration_chart_map)
     + (SELECT COUNT(*) FROM playdata) - (SELECT COUNT(*) FROM migration_playdata_map),
       CURRENT_TIMESTAMP;

SELECT check_name, failure_count
FROM migration_verification_results
ORDER BY check_name;

SELECT source_table, reason_code, COUNT(*) AS failure_count
FROM migration_failures
GROUP BY source_table, reason_code
ORDER BY source_table, reason_code;

SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'user_profiles', COUNT(*) FROM user_profiles
UNION ALL SELECT 'songs', COUNT(*) FROM songs
UNION ALL SELECT 'charts', COUNT(*) FROM charts
UNION ALL SELECT 'playdata', COUNT(*) FROM playdata
UNION ALL SELECT 'playdata_history', COUNT(*) FROM playdata_history;
