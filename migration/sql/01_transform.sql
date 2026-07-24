USE `__TARGET_DB__`;
SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE IF NOT EXISTS migration_sessions (
  session_id VARCHAR(64) PRIMARY KEY,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME NOT NULL,
  finished_at DATETIME NULL,
  input_count BIGINT NOT NULL DEFAULT 0,
  success_count BIGINT NOT NULL DEFAULT 0,
  failure_count BIGINT NOT NULL DEFAULT 0,
  skip_count BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS migration_user_map (
  session_id VARCHAR(64) NOT NULL, old_user_id BIGINT NOT NULL, new_user_id BIGINT NOT NULL,
  PRIMARY KEY (session_id, old_user_id)
);
CREATE TABLE IF NOT EXISTS migration_song_map (
  session_id VARCHAR(64) NOT NULL, old_chart_id BIGINT NOT NULL,
  old_song_hash VARCHAR(255) NULL, new_song_id BIGINT NOT NULL, new_song_hash VARCHAR(255) NULL,
  PRIMARY KEY (session_id, old_chart_id)
);
CREATE TABLE IF NOT EXISTS migration_chart_map (
  session_id VARCHAR(64) NOT NULL, old_chart_id BIGINT NOT NULL, new_chart_id BIGINT NOT NULL,
  PRIMARY KEY (session_id, old_chart_id)
);
CREATE TABLE IF NOT EXISTS migration_playdata_map (
  session_id VARCHAR(64) NOT NULL, old_playdata_id BIGINT NOT NULL,
  new_playdata_id BIGINT NOT NULL, PRIMARY KEY (session_id, old_playdata_id)
);
CREATE TABLE IF NOT EXISTS migration_failures (
  failure_id BIGINT AUTO_INCREMENT PRIMARY KEY, session_id VARCHAR(64) NOT NULL,
  source_table VARCHAR(32) NOT NULL, source_id BIGINT NULL, reason_code VARCHAR(64) NOT NULL,
  KEY idx_migration_failure_session_reason (session_id, reason_code)
);

INSERT INTO migration_sessions (session_id, status, started_at)
VALUES ('__SESSION_ID__', 'RUNNING', CURRENT_TIMESTAMP);
START TRANSACTION;

INSERT INTO migration_failures (session_id, source_table, source_id, reason_code)
SELECT '__SESSION_ID__', 'user', user_id, 'MISSING_REQUIRED_ACCOUNT_FIELD'
FROM `__LEGACY_DB__`.`user`
WHERE poptomo_id IS NULL OR poptomo_id = '' OR password IS NULL;

INSERT INTO users (user_id, poptomo_id, password_hash, role, created_at, updated_at)
SELECT user_id, poptomo_id, password, COALESCE(NULLIF(role, ''), 'USER'),
       COALESCE(created_at, CURRENT_TIMESTAMP),
       COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.`user`
WHERE poptomo_id IS NOT NULL AND poptomo_id <> '' AND password IS NOT NULL;
INSERT INTO migration_user_map
SELECT '__SESSION_ID__', user_id, user_id FROM users;

INSERT INTO user_profiles (
  user_id, user_name, character_name, comment, is_hidden,
  display_popclass, potential_popclass, legacy_popclass,
  normal_credit, extra_credit, time_play_10_credit, time_play_16_credit,
  created_at, updated_at
)
SELECT u.user_id, COALESCE(NULLIF(u.user_name, ''), CONCAT('user-', u.user_id)),
       COALESCE(u.`character`, ''), COALESCE(u.comment, ''), COALESCE(u.is_hidden, 0),
       0, 0, COALESCE(u.popclass, 0), 0, 0, 0, 0,
       COALESCE(u.created_at, CURRENT_TIMESTAMP),
       COALESCE(u.updated_at, u.created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.`user` u
JOIN migration_user_map m ON m.session_id = '__SESSION_ID__'
 AND m.old_user_id = u.user_id;

CREATE TEMPORARY TABLE migration_song_source AS
SELECT grouped.*,
       ROW_NUMBER() OVER (ORDER BY representative_chart_id) AS generated_song_id
FROM (
  SELECT COALESCE(NULLIF(song_hash, ''),
           SHA2(CONCAT_WS('|', genre_name, song_name, version), 256)) AS group_key,
         MIN(chart_id) AS representative_chart_id
  FROM `__LEGACY_DB__`.chart
  GROUP BY group_key
) grouped;
INSERT INTO songs (song_hash, genre_name, song_name, version, jacket_url, created_at, updated_at)
SELECT NULLIF(c.song_hash, ''), COALESCE(NULLIF(c.genre_name, ''), 'UNKNOWN'),
       COALESCE(NULLIF(c.song_name, ''), 'UNKNOWN'), COALESCE(c.version, 0),
       NULLIF(c.jacket, ''), COALESCE(c.created_at, CURRENT_TIMESTAMP),
       COALESCE(c.created_at, CURRENT_TIMESTAMP)
FROM migration_song_source g
JOIN `__LEGACY_DB__`.chart c ON c.chart_id = g.representative_chart_id
ORDER BY g.representative_chart_id;
INSERT INTO migration_song_map
SELECT '__SESSION_ID__', c.chart_id, NULLIF(c.song_hash, ''), s.song_id, s.song_hash
FROM `__LEGACY_DB__`.chart c
JOIN migration_song_source g
  ON g.group_key = COALESCE(NULLIF(c.song_hash, ''),
       SHA2(CONCAT_WS('|', c.genre_name, c.song_name, c.version), 256))
JOIN songs s ON s.song_id = g.generated_song_id;

INSERT INTO migration_failures (session_id, source_table, source_id, reason_code)
SELECT '__SESSION_ID__', 'chart', chart_id, 'INVALID_CHART_SHAPE'
FROM `__LEGACY_DB__`.chart
WHERE difficulty NOT BETWEEN 1 AND 4 OR level NOT BETWEEN 1 AND 50;
INSERT INTO charts (
  chart_id, song_id, difficulty_code, difficulty_label, level, chart_version,
  is_upper, is_deleted, created_at, updated_at
)
SELECT c.chart_id, sm.new_song_id, c.difficulty,
       ELT(c.difficulty, 'LIGHT', 'NORMAL', 'HYPER', 'EX'),
       c.level, COALESCE(c.version, 0), COALESCE(c.is_upper, 0),
       COALESCE(c.is_deleted, 0), COALESCE(c.created_at, CURRENT_TIMESTAMP),
       COALESCE(c.created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.chart c
JOIN migration_song_map sm ON sm.session_id = '__SESSION_ID__'
 AND sm.old_chart_id = c.chart_id
WHERE c.difficulty BETWEEN 1 AND 4 AND c.level BETWEEN 1 AND 50;
INSERT INTO migration_chart_map
SELECT '__SESSION_ID__', chart_id, chart_id FROM charts;

CREATE TEMPORARY TABLE migration_ranked_playdata AS
SELECT p.*, ROW_NUMBER() OVER (
  PARTITION BY p.user_id, p.chart_id ORDER BY COALESCE(p.score, 0) DESC, p.playdata_id DESC
) AS candidate_rank
FROM `__LEGACY_DB__`.playdata p;
INSERT INTO migration_failures (session_id, source_table, source_id, reason_code)
SELECT '__SESSION_ID__', 'playdata', playdata_id,
       CASE WHEN candidate_rank > 1 THEN 'DUPLICATE_USER_CHART'
            WHEN score IS NULL OR score < 0 THEN 'INVALID_SCORE'
            WHEN `rank` IS NULL THEN 'MISSING_SOURCE_RANK'
            WHEN medal IS NULL THEN 'MISSING_SOURCE_MEDAL'
            ELSE 'IDENTITY_NOT_MAPPED' END
FROM migration_ranked_playdata p
LEFT JOIN migration_user_map um ON um.session_id = '__SESSION_ID__' AND um.old_user_id = p.user_id
LEFT JOIN migration_chart_map cm ON cm.session_id = '__SESSION_ID__' AND cm.old_chart_id = p.chart_id
WHERE candidate_rank > 1 OR score IS NULL OR score < 0 OR `rank` IS NULL OR medal IS NULL
   OR um.new_user_id IS NULL OR cm.new_chart_id IS NULL;

INSERT INTO playdata (
  playdata_id, user_id, chart_id, current_version, version_score, version_rank_code,
  all_time_score, all_time_score_version, all_time_rank_code, medal_code,
  popclass, created_at, updated_at
)
SELECT p.playdata_id, um.new_user_id, cm.new_chart_id, 28, p.score, p.`rank`,
       p.score, 28, p.`rank`, p.medal, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM migration_ranked_playdata p
JOIN migration_user_map um ON um.session_id = '__SESSION_ID__' AND um.old_user_id = p.user_id
JOIN migration_chart_map cm ON cm.session_id = '__SESSION_ID__' AND cm.old_chart_id = p.chart_id
WHERE p.candidate_rank = 1 AND p.score >= 0 AND p.`rank` IS NOT NULL AND p.medal IS NOT NULL;
INSERT INTO migration_playdata_map
SELECT '__SESSION_ID__', playdata_id, playdata_id FROM playdata;

UPDATE migration_sessions
SET status = 'SUCCESS', finished_at = CURRENT_TIMESTAMP,
    input_count = (SELECT COUNT(*) FROM `__LEGACY_DB__`.playdata),
    success_count = (SELECT COUNT(*) FROM migration_playdata_map WHERE session_id='__SESSION_ID__'),
    failure_count = (SELECT COUNT(*) FROM migration_failures WHERE session_id='__SESSION_ID__'),
    skip_count = (SELECT COUNT(*) FROM migration_failures
                  WHERE session_id='__SESSION_ID__' AND reason_code='DUPLICATE_USER_CHART')
WHERE session_id = '__SESSION_ID__';
COMMIT;
