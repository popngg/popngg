USE `__TARGET_DB__`;
SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE IF NOT EXISTS migration_verification_results (
  session_id VARCHAR(64) NOT NULL,
  check_name VARCHAR(64) NOT NULL,
  category VARCHAR(32) NOT NULL,
  severity VARCHAR(16) NOT NULL,
  expected_count BIGINT NOT NULL,
  actual_count BIGINT NOT NULL,
  status VARCHAR(8) NOT NULL,
  checked_at DATETIME NOT NULL,
  PRIMARY KEY (session_id, check_name)
);
DELETE FROM migration_verification_results WHERE session_id = '__SESSION_ID__';

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'users_row_count', 'ROW_COUNT', 'BLOCKER',
       (SELECT COUNT(*) FROM `__LEGACY_DB__`.`user`
         WHERE poptomo_id IS NOT NULL AND poptomo_id <> '' AND password IS NOT NULL),
       (SELECT COUNT(*) FROM migration_user_map WHERE session_id='__SESSION_ID__'),
       IF((SELECT COUNT(*) FROM `__LEGACY_DB__`.`user`
            WHERE poptomo_id IS NOT NULL AND poptomo_id <> '' AND password IS NOT NULL)
          = (SELECT COUNT(*) FROM migration_user_map WHERE session_id='__SESSION_ID__'),
          'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'user_profiles_row_count', 'ROW_COUNT', 'BLOCKER',
       (SELECT COUNT(*) FROM migration_user_map WHERE session_id='__SESSION_ID__'),
       (SELECT COUNT(*) FROM user_profiles),
       IF((SELECT COUNT(*) FROM migration_user_map WHERE session_id='__SESSION_ID__')
          = (SELECT COUNT(*) FROM user_profiles), 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'songs_row_count', 'ROW_COUNT', 'BLOCKER',
       (SELECT COUNT(DISTINCT COALESCE(NULLIF(song_hash, ''),
                 SHA2(CONCAT_WS('|', genre_name, song_name, version), 256)))
          FROM `__LEGACY_DB__`.chart),
       (SELECT COUNT(*) FROM songs),
       IF((SELECT COUNT(DISTINCT COALESCE(NULLIF(song_hash, ''),
                 SHA2(CONCAT_WS('|', genre_name, song_name, version), 256)))
            FROM `__LEGACY_DB__`.chart) = (SELECT COUNT(*) FROM songs),
          'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'charts_row_count', 'ROW_COUNT', 'BLOCKER',
       (SELECT COUNT(*) FROM `__LEGACY_DB__`.chart
         WHERE difficulty BETWEEN 1 AND 4 AND level BETWEEN 1 AND 50),
       (SELECT COUNT(*) FROM charts),
       IF((SELECT COUNT(*) FROM `__LEGACY_DB__`.chart
            WHERE difficulty BETWEEN 1 AND 4 AND level BETWEEN 1 AND 50)
          = (SELECT COUNT(*) FROM charts), 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'playdata_row_count', 'ROW_COUNT', 'BLOCKER',
       (SELECT success_count FROM migration_sessions WHERE session_id='__SESSION_ID__'),
       (SELECT COUNT(*) FROM playdata),
       IF((SELECT success_count FROM migration_sessions WHERE session_id='__SESSION_ID__')
          = (SELECT COUNT(*) FROM playdata), 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'users_profiles_orphans', 'ORPHAN', 'BLOCKER', 0,
       (SELECT COUNT(*) FROM users u LEFT JOIN user_profiles p ON p.user_id=u.user_id
         WHERE p.user_id IS NULL)
       + (SELECT COUNT(*) FROM user_profiles p LEFT JOIN users u ON u.user_id=p.user_id
           WHERE u.user_id IS NULL),
       IF(((SELECT COUNT(*) FROM users u LEFT JOIN user_profiles p ON p.user_id=u.user_id
              WHERE p.user_id IS NULL)
           + (SELECT COUNT(*) FROM user_profiles p LEFT JOIN users u ON u.user_id=p.user_id
               WHERE u.user_id IS NULL)) = 0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'catalog_and_playdata_orphans', 'ORPHAN', 'BLOCKER', 0,
       (SELECT COUNT(*) FROM charts c LEFT JOIN songs s ON s.song_id=c.song_id
         WHERE s.song_id IS NULL)
       + (SELECT COUNT(*) FROM playdata p LEFT JOIN users u ON u.user_id=p.user_id
           WHERE u.user_id IS NULL)
       + (SELECT COUNT(*) FROM playdata p LEFT JOIN charts c ON c.chart_id=p.chart_id
           WHERE c.chart_id IS NULL),
       IF(((SELECT COUNT(*) FROM charts c LEFT JOIN songs s ON s.song_id=c.song_id
              WHERE s.song_id IS NULL)
           + (SELECT COUNT(*) FROM playdata p LEFT JOIN users u ON u.user_id=p.user_id
               WHERE u.user_id IS NULL)
           + (SELECT COUNT(*) FROM playdata p LEFT JOIN charts c ON c.chart_id=p.chart_id
               WHERE c.chart_id IS NULL)) = 0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'user_chart_duplicates', 'UNIQUE', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM (SELECT user_id, chart_id FROM playdata GROUP BY user_id, chart_id HAVING COUNT(*) > 1) d;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'legacy_popclass_mismatch', 'POPCLASS', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM migration_user_map m
JOIN `__LEGACY_DB__`.`user` old_user ON old_user.user_id=m.old_user_id
JOIN user_profiles p ON p.user_id=m.new_user_id
WHERE m.session_id='__SESSION_ID__'
  AND p.legacy_popclass <> COALESCE(old_user.popclass, 0);

INSERT INTO migration_verification_results
WITH chart_values AS (
  SELECT p.user_id,
         GREATEST(0, FLOOR((c.level * 10000 + p.all_time_score - 50000
           + CASE WHEN p.medal_code BETWEEN 1 AND 4 THEN 5000
                  WHEN p.medal_code BETWEEN 5 AND 8 THEN 3000 ELSE 0 END) / 54.4)) AS chart_popclass,
         ROW_NUMBER() OVER (
           PARTITION BY p.user_id
           ORDER BY GREATEST(0, FLOOR((c.level * 10000 + p.all_time_score - 50000
             + CASE WHEN p.medal_code BETWEEN 1 AND 4 THEN 5000
                    WHEN p.medal_code BETWEEN 5 AND 8 THEN 3000 ELSE 0 END) / 54.4)) DESC,
                    p.all_time_score DESC, p.chart_id) AS value_rank
    FROM playdata p JOIN charts c ON c.chart_id=p.chart_id
   WHERE c.is_deleted=FALSE
), expected AS (
  SELECT user_id, FLOOR(SUM(chart_popclass) / 50) AS potential_popclass
    FROM chart_values WHERE value_rank <= 50 GROUP BY user_id
)
SELECT '__SESSION_ID__', 'potential_popclass_mismatch', 'POPCLASS', 'BLOCKER', 0,
       COUNT(*), IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM expected e JOIN user_profiles p ON p.user_id=e.user_id
WHERE p.potential_popclass <> e.potential_popclass;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'display_popclass_invalid', 'POPCLASS', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM user_profiles
WHERE display_popclass < 0 OR potential_popclass < 0
   OR display_popclass > potential_popclass;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'new_credits_nonzero', 'CREDIT', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM user_profiles
WHERE normal_credit <> 0 OR extra_credit <> 0
   OR time_play_10_credit <> 0 OR time_play_16_credit <> 0;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'song_hash_duplicate_targets', 'SONG_HASH', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM (SELECT song_hash FROM songs WHERE song_hash IS NOT NULL
       GROUP BY song_hash HAVING COUNT(*) > 1) duplicates;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'song_hash_alias_split', 'SONG_HASH', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM (SELECT old_song_hash FROM migration_song_map
       WHERE session_id='__SESSION_ID__' AND old_song_hash IS NOT NULL
       GROUP BY old_song_hash HAVING COUNT(DISTINCT new_song_id) > 1) aliases;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'song_mapping_missing', 'MAPPING', 'BLOCKER', 0,
       (SELECT COUNT(*) FROM `__LEGACY_DB__`.chart old_chart
         LEFT JOIN migration_song_map m
           ON m.session_id='__SESSION_ID__' AND m.old_chart_id=old_chart.chart_id
        WHERE m.old_chart_id IS NULL),
       IF((SELECT COUNT(*) FROM `__LEGACY_DB__`.chart old_chart
            LEFT JOIN migration_song_map m
              ON m.session_id='__SESSION_ID__' AND m.old_chart_id=old_chart.chart_id
           WHERE m.old_chart_id IS NULL)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP;

INSERT INTO migration_verification_results
SELECT '__SESSION_ID__', 'jacket_reference_mismatch', 'JACKET', 'BLOCKER', 0, COUNT(*),
       IF(COUNT(*)=0, 'PASS', 'FAIL'), CURRENT_TIMESTAMP
FROM migration_song_map m
JOIN `__LEGACY_DB__`.chart old_chart ON old_chart.chart_id=m.old_chart_id
JOIN songs s ON s.song_id=m.new_song_id
WHERE m.session_id='__SESSION_ID__'
  AND BINARY COALESCE(NULLIF(old_chart.jacket, ''), '')
      <> BINARY COALESCE(s.jacket_url, '');

SELECT check_name, category, severity, expected_count, actual_count, status
FROM migration_verification_results
WHERE session_id='__SESSION_ID__'
ORDER BY FIELD(severity, 'BLOCKER', 'WARNING'), category, check_name;
