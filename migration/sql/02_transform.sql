USE `__TARGET_DB__`;
SET SESSION sql_mode = 'STRICT_ALL_TABLES,NO_ENGINE_SUBSTITUTION';
START TRANSACTION;

INSERT INTO migration_failures (source_table, source_id, reason_code)
SELECT 'user', user_id, 'MISSING_REQUIRED_ACCOUNT_FIELD'
FROM `__LEGACY_DB__`.`user`
WHERE poptomo_id IS NULL OR poptomo_id = '' OR password IS NULL;

INSERT INTO users (
  user_id, poptomo_id, password_hash, email, email_verified_at, role,
  created_at, updated_at
)
SELECT user_id, poptomo_id, password, NULL, NULL,
       COALESCE(NULLIF(role, ''), 'USER'),
       COALESCE(created_at, CURRENT_TIMESTAMP),
       COALESCE(updated_at, created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.`user`
WHERE poptomo_id IS NOT NULL AND poptomo_id <> '' AND password IS NOT NULL;

INSERT INTO migration_user_map (old_user_id, new_user_id)
SELECT user_id, user_id FROM users;

INSERT INTO user_profiles (
  user_id, user_name, character_name, comment, profile_image_url, is_hidden,
  display_popclass, potential_popclass, legacy_popclass,
  normal_credit, extra_credit, time_play_10_credit, time_play_16_credit,
  created_at, updated_at
)
SELECT u.user_id, COALESCE(NULLIF(u.user_name, ''), CONCAT('user-', u.user_id)),
       COALESCE(u.`character`, ''), COALESCE(u.comment, ''), NULL,
       COALESCE(u.is_hidden, 0) <> 0,
       0, 0, COALESCE(u.popclass, 0),
       0, 0, 0, 0,
       COALESCE(u.created_at, CURRENT_TIMESTAMP),
       COALESCE(u.updated_at, u.created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.`user` u
JOIN migration_user_map m ON m.old_user_id = u.user_id;

CREATE TEMPORARY TABLE song_groups AS
SELECT
  COALESCE(NULLIF(song_hash, ''),
           SHA2(CONCAT_WS('|', COALESCE(genre_name, ''), COALESCE(song_name, ''),
                          COALESCE(version, 0)), 256)) AS group_key,
  MIN(chart_id) AS representative_chart_id
FROM `__LEGACY_DB__`.chart
GROUP BY group_key;

INSERT INTO songs (
  song_hash, genre_name, song_name, artist_name, version, jacket_url,
  created_at, updated_at
)
SELECT NULLIF(c.song_hash, ''),
       COALESCE(NULLIF(c.genre_name, ''), COALESCE(NULLIF(c.song_name, ''), 'UNKNOWN')),
       COALESCE(NULLIF(c.song_name, ''), 'UNKNOWN'),
       NULL, COALESCE(c.version, 0), NULLIF(c.jacket, ''),
       COALESCE(c.created_at, CURRENT_TIMESTAMP),
       COALESCE(c.created_at, CURRENT_TIMESTAMP)
FROM song_groups g
JOIN `__LEGACY_DB__`.chart c ON c.chart_id = g.representative_chart_id
ORDER BY g.representative_chart_id;

CREATE TEMPORARY TABLE song_group_ids AS
SELECT g.group_key, s.song_id
FROM (
  SELECT group_key, representative_chart_id,
         ROW_NUMBER() OVER (ORDER BY representative_chart_id) AS generated_id
  FROM song_groups
) g
JOIN songs s ON s.song_id = g.generated_id;

INSERT INTO migration_song_map (
  old_chart_id, old_song_hash, new_song_id, new_song_hash
)
SELECT c.chart_id, NULLIF(c.song_hash, ''), sg.song_id, s.song_hash
FROM `__LEGACY_DB__`.chart c
JOIN song_group_ids sg
  ON sg.group_key = COALESCE(NULLIF(c.song_hash, ''),
       SHA2(CONCAT_WS('|', COALESCE(c.genre_name, ''), COALESCE(c.song_name, ''),
                      COALESCE(c.version, 0)), 256))
JOIN songs s ON s.song_id = sg.song_id;

INSERT INTO migration_failures (source_table, source_id, reason_code)
SELECT 'chart', chart_id, 'INVALID_CHART_SHAPE'
FROM `__LEGACY_DB__`.chart
WHERE difficulty NOT BETWEEN 1 AND 4 OR level NOT BETWEEN 1 AND 50;

INSERT INTO charts (
  chart_id, song_id, difficulty_code, difficulty_label, level, chart_version,
  has_strict_judgement, has_strict_gauge, is_upper, is_deleted,
  created_at, updated_at
)
SELECT c.chart_id, sm.new_song_id, c.difficulty,
       CASE c.difficulty
         WHEN 1 THEN 'LIGHT' WHEN 2 THEN 'NORMAL'
         WHEN 3 THEN 'HYPER' WHEN 4 THEN 'EX'
       END,
       c.level, COALESCE(c.version, 0), FALSE, FALSE,
       COALESCE(c.is_upper, 0) <> 0, COALESCE(c.is_deleted, 0) <> 0,
       COALESCE(c.created_at, CURRENT_TIMESTAMP),
       COALESCE(c.created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.chart c
JOIN migration_song_map sm ON sm.old_chart_id = c.chart_id
WHERE c.difficulty BETWEEN 1 AND 4 AND c.level BETWEEN 1 AND 50;

INSERT INTO migration_chart_map (old_chart_id, new_chart_id)
SELECT chart_id, chart_id FROM charts;

CREATE TEMPORARY TABLE ranked_playdata AS
SELECT p.*,
       ROW_NUMBER() OVER (
         PARTITION BY p.user_id, p.chart_id
         ORDER BY COALESCE(p.score, 0) DESC, p.playdata_id DESC
       ) AS candidate_rank
FROM `__LEGACY_DB__`.playdata p;

INSERT INTO migration_failures (source_table, source_id, reason_code)
SELECT 'playdata', p.playdata_id,
       CASE
         WHEN um.new_user_id IS NULL THEN 'USER_NOT_MAPPED'
         WHEN cm.new_chart_id IS NULL THEN 'CHART_NOT_MAPPED'
         WHEN p.score IS NULL OR p.score < 0 THEN 'INVALID_SCORE'
         WHEN p.rank IS NULL THEN 'MISSING_SOURCE_RANK'
         WHEN p.medal IS NULL THEN 'MISSING_SOURCE_MEDAL'
         ELSE 'DUPLICATE_USER_CHART'
       END
FROM ranked_playdata p
LEFT JOIN migration_user_map um ON um.old_user_id = p.user_id
LEFT JOIN migration_chart_map cm ON cm.old_chart_id = p.chart_id
WHERE um.new_user_id IS NULL OR cm.new_chart_id IS NULL
   OR p.score IS NULL OR p.score < 0 OR p.rank IS NULL OR p.medal IS NULL
   OR p.candidate_rank > 1;

INSERT INTO playdata (
  playdata_id, user_id, chart_id, current_version,
  version_score, version_rank_code, all_time_score, all_time_score_version,
  all_time_rank_code, medal_code, popclass,
  created_at, updated_at
)
SELECT p.playdata_id, um.new_user_id, cm.new_chart_id, 28,
       p.score, p.rank, p.score, 28, p.rank, p.medal,
       GREATEST(0, FLOOR((
         c.level * 10000 + p.score - 50000 +
         CASE WHEN p.medal BETWEEN 1 AND 4 THEN 5000
              WHEN p.medal BETWEEN 5 AND 8 THEN 3000 ELSE 0 END
       ) / 54.4)),
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM ranked_playdata p
JOIN migration_user_map um ON um.old_user_id = p.user_id
JOIN migration_chart_map cm ON cm.old_chart_id = p.chart_id
JOIN charts c ON c.chart_id = cm.new_chart_id
WHERE p.candidate_rank = 1 AND p.score >= 0
  AND p.rank IS NOT NULL AND p.medal IS NOT NULL;

INSERT INTO migration_playdata_map (old_playdata_id, new_playdata_id)
SELECT playdata_id, playdata_id FROM playdata;

INSERT INTO playdata_history (
  history_id, user_id, chart_id, game_version,
  previous_version_score, version_score,
  previous_all_time_score, all_time_score,
  previous_rank_code, rank_code, previous_medal_code, medal_code,
  popclass, event_type, renew_log_id, created_at
)
SELECT h.history_id, um.new_user_id, cm.new_chart_id, 28,
       NULL, h.score, NULL, h.score, NULL, h.rank, NULL, h.medal,
       h.popclass, 'MIGRATION', NULL, COALESCE(h.created_at, CURRENT_TIMESTAMP)
FROM `__LEGACY_DB__`.history h
JOIN migration_user_map um ON um.old_user_id = h.user_id
JOIN migration_chart_map cm ON cm.old_chart_id = h.chart_id;

CREATE TEMPORARY TABLE potential_ranks AS
SELECT user_id, popclass,
       ROW_NUMBER() OVER (
         PARTITION BY user_id ORDER BY all_time_score DESC, popclass DESC, playdata_id
       ) AS score_rank
FROM playdata;

UPDATE user_profiles up
JOIN (
  SELECT user_id, FLOOR(SUM(CASE WHEN score_rank <= 50 THEN popclass ELSE 0 END) / 50) AS value
  FROM potential_ranks
  GROUP BY user_id
) p ON p.user_id = up.user_id
SET up.potential_popclass = p.value,
    up.display_popclass = p.value;

COMMIT;
