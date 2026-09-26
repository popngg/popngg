-- Manual pilot only. Replace __TARGET_DB__ with the reviewed database name.
-- Execute with mysql --default-character-set=utf8mb4, without --force.
-- The song hash and title checks prevent a song_id reused in another catalog
-- from receiving an unrelated alias. This is not a Flyway migration.
SET SESSION sql_mode = 'STRICT_ALL_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
CREATE TEMPORARY TABLE pilot_song_aliases (
    song_id BIGINT NOT NULL,
    song_hash CHAR(64) NOT NULL,
    song_name VARCHAR(255) NOT NULL,
    tag_value VARCHAR(255) NOT NULL,
    normalized_tag_value VARCHAR(255) NOT NULL,
    tag_type VARCHAR(32) NOT NULL,
    PRIMARY KEY (song_id, normalized_tag_value, tag_type)
);
INSERT INTO pilot_song_aliases VALUES
    (477, '2069b8e308e8c524e99c0101ff2d5c84122e6edf4bef4338a2ed136e49bd6110', 'シュレーディンガーの猫', '슈뢰딩거의 고양이', '슈뢰딩거의 고양이', 'KO_ALIAS'),
    (477, '2069b8e308e8c524e99c0101ff2d5c84122e6edf4bef4338a2ed136e49bd6110', 'シュレーディンガーの猫', 'Schrodingers Cat', 'schrodingers cat', 'EN_ALIAS'),
    (530, '7aa1d5ff1c24adf7843ac69a7ee2e373c49a0597cd311db27592b205a998379a', '音楽', '음악', '음악', 'KO_ALIAS'),
    (530, '7aa1d5ff1c24adf7843ac69a7ee2e373c49a0597cd311db27592b205a998379a', '音楽', 'Ongaku', 'ongaku', 'EN_ALIAS'),
    (714, '85f5498bee0345ee82f5d399bfbe37d6696cf7139cabf9eb189e4e76aff49139', 'ピアノ協奏曲第１番”蠍火”', '피아노 협주곡 제1번 사소리비', '피아노 협주곡 제1번 사소리비', 'KO_ALIAS'),
    (714, '85f5498bee0345ee82f5d399bfbe37d6696cf7139cabf9eb189e4e76aff49139', 'ピアノ協奏曲第１番”蠍火”', 'Sasoribi', 'sasoribi', 'EN_ALIAS'),
    (899, '34e2be66d9af2585163f79363f0a678b3231671ba769f4578a3406645a4b1a26', 'L-an!ma', '라니마', '라니마', 'KO_ALIAS'),
    (1106, 'f3dbcff541f141514896933d0b888a2b781df2a66023235019e8b915495f7b9e', 'Chaos:Q', '카오스 큐', '카오스 큐', 'KO_ALIAS'),
    (1246, 'd2d6694b67ba82dbf0a84dcc4f90c71efd1c4d4605ef4a8b801fff8db9f355f4', 'BabeL ～MODEL DD101～', '바벨 모델 DD101', '바벨 모델 dd101', 'KO_ALIAS'),
    (1255, 'a8019f9e2c4ccd1cc447a1dfdb7232071fb492e6287d12666f6c77c67738da8f', 'Blue River', '블루 리버', '블루 리버', 'KO_ALIAS'),
    (1415, '90a95eca0e90eff8465d3c78526ad88424ccea961b5caebc7d32c4b81a1ceca7', 'ΔΟΓΜΑ', '도그마', '도그마', 'KO_ALIAS'),
    (1415, '90a95eca0e90eff8465d3c78526ad88424ccea961b5caebc7d32c4b81a1ceca7', 'ΔΟΓΜΑ', 'Dogma', 'dogma', 'EN_ALIAS'),
    (1557, '63b0dfa997cd530697cf9debd407ae319356679bab933ea57657bdaa315ab31b', 'Mecha Kawa Breaker!!', '메카 카와 브레이커', '메카 카와 브레이커', 'KO_ALIAS'),
    (1890, '63a18ae637230d55e6e0d7f2aa9a65ed92e4b83e44141c45e6a1103016b4db45', 'Megalara Garuda', '메갈라라 가루다', '메갈라라 가루다', 'KO_ALIAS');

CREATE TEMPORARY TABLE pilot_matched AS
SELECT a.song_id, a.tag_value, a.normalized_tag_value, a.tag_type
  FROM pilot_song_aliases a
  JOIN `__TARGET_DB__`.songs s
    ON s.song_id = a.song_id AND s.song_hash = a.song_hash
   AND s.song_name = a.song_name
 WHERE EXISTS (SELECT 1 FROM `__TARGET_DB__`.charts c
                WHERE c.song_id = s.song_id AND c.level = 50
                  AND c.is_deleted = FALSE);
CREATE TEMPORARY TABLE pilot_assertion (ok INT NOT NULL);
INSERT INTO pilot_assertion (ok)
SELECT IF(COUNT(*) = 14 AND COUNT(DISTINCT song_id) = 10, 1, NULL)
  FROM pilot_matched;
INSERT INTO pilot_assertion (ok)
SELECT IF(COUNT(*) = 0, 1, NULL)
  FROM pilot_matched m
  JOIN `__TARGET_DB__`.song_search_tags existing
    ON existing.song_id = m.song_id
   AND existing.normalized_tag_value = m.normalized_tag_value
   AND existing.tag_type = m.tag_type
 WHERE existing.source <> 'PILOT_20260927'
    OR existing.tag_value <> m.tag_value;

START TRANSACTION;
UPDATE `__TARGET_DB__`.song_search_tags existing
JOIN pilot_matched m
  ON existing.song_id = m.song_id
 AND existing.normalized_tag_value = m.normalized_tag_value
 AND existing.tag_type = m.tag_type
   SET existing.is_active = TRUE,
       existing.updated_at = CURRENT_TIMESTAMP
 WHERE existing.source = 'PILOT_20260927';
INSERT IGNORE INTO `__TARGET_DB__`.song_search_tags
    (song_id, tag_value, normalized_tag_value, tag_type, source,
     is_active, created_at, updated_at)
SELECT song_id, tag_value, normalized_tag_value, tag_type,
       'PILOT_20260927', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM pilot_matched;
INSERT INTO pilot_assertion (ok)
SELECT IF(COUNT(*) = 14 AND SUM(is_active) = 14
          AND COUNT(DISTINCT song_id) = 10, 1, NULL)
  FROM `__TARGET_DB__`.song_search_tags
 WHERE source = 'PILOT_20260927';
COMMIT;
SELECT 'PILOT_TAGS_ACTIVE' AS result, COUNT(*) AS tag_count
  FROM `__TARGET_DB__`.song_search_tags
 WHERE source = 'PILOT_20260927' AND is_active = TRUE;
