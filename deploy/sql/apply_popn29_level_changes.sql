-- Apply the 91 chart-level changes announced for pop'n music High Cheers.
-- Chart IDs are preserved so existing playdata and history remain attached.
START TRANSACTION;

CREATE TEMPORARY TABLE popn29_level_updates (
  chart_id BIGINT PRIMARY KEY,
  chart_name VARCHAR(255) NOT NULL,
  difficulty_label VARCHAR(16) NOT NULL,
  old_level TINYINT NOT NULL,
  new_level TINYINT NOT NULL
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO popn29_level_updates VALUES
(7, 'ポップス / I REALLY WANT TO HURT YOU', 'LIGHT', 1, 3),
(1329, '月光花', 'LIGHT', 1, 2),
(15, 'Ｊ‐テクノ / Quick Master', 'LIGHT', 2, 4),
(146, 'アニメヒロイン / 魔法の扉(スペース●マコのテーマ)', 'LIGHT', 2, 4),
(761, '残酷な天使のテーゼ', 'LIGHT', 2, 6),
(1507, 'キセキ', 'LIGHT', 2, 8),
(78, 'フレンドリー / Over The Rainbow', 'LIGHT', 3, 5),
(1549, 'ブルーバード', 'LIGHT', 3, 7),
(2205, 'ドラムステップ / Empathetic', 'LIGHT', 3, 5),
(2621, '青春剛速球メタル / マインド・ゲーム', 'LIGHT', 3, 5),
(4072, 'マトリョシカ', 'LIGHT', 3, 6),
(2526, 'セツナトリップ', 'LIGHT', 6, 8),
(6955, '曇天(UPPER)', 'NORMAL', 29, 34),
(766, 'ラメント / 雫', 'NORMAL', 30, 31),
(452, 'オイパンク０ / ブタパンチのテーマ', 'NORMAL', 31, 32),
(2829, 'perditus†paradisus', 'NORMAL', 31, 32),
(5425, 'LIMIT TOPPA REVOLUTION', 'NORMAL', 31, 32),
(5457, 'Satan', 'NORMAL', 31, 32),
(4381, 'perditus†paradisus(UPPER)', 'NORMAL', 32, 34),
(3629, 'Chaos:Q', 'NORMAL', 33, 35),
(6214, 'ツッパリ / バリバリブギ ～涙のフルーツポンチ～', 'HYPER', 29, 31),
(144, 'パーカッシヴ / 西新宿清掃曲', 'HYPER', 30, 31),
(5907, 'ファンクロック / 熟れた花', 'HYPER', 31, 32),
(262, 'ダークネス / 電気人形', 'HYPER', 33, 34),
(785, 'ビワガタリ / 涙雨物語', 'HYPER', 34, 36),
(216, 'チアガール / GET THE CHANCE！', 'HYPER', 35, 36),
(3598, 'Arcanos', 'HYPER', 36, 37),
(257, 'キョウゲキ / 加油！元気猿！', 'HYPER', 37, 38),
(1474, 'マダーロック / Treasure Hoard', 'HYPER', 39, 40),
(5756, 'グリーニング / Greening', 'HYPER', 39, 40),
(3446, 'サケビノミドリ', 'HYPER', 39, 40),
(650, 'ナニワヒーロー / でんがなマンガナ', 'HYPER', 40, 41),
(1374, 'グロッソラリア / 万物快楽理論', 'HYPER', 40, 41),
(1436, 'ルナティックリール / moon dance', 'HYPER', 40, 41),
(1560, '流星RAVE REMIX / 流星☆ハニー Perforation Mix', 'HYPER', 40, 41),
(1453, 'ハンズアップ / Second Heaven', 'HYPER', 40, 41),
(2750, 'Habits', 'HYPER', 40, 41),
(2726, 'Dimension Gale', 'HYPER', 40, 41),
(4282, 'Floccinaucinihilipilification', 'HYPER', 40, 41),
(5234, '天和無双', 'HYPER', 40, 41),
(5834, 'クラシック２ / R.C.', 'HYPER', 41, 42),
(682, 'ビビッド / For Dear ～', 'HYPER', 41, 42),
(779, 'ロックビリー / ススメ！少年！！', 'HYPER', 41, 42),
(739, 'ハイパーロッケンローレ / エイプリルフールの唄', 'HYPER', 41, 42),
(817, 'ハードＰｆ / fffff', 'HYPER', 41, 42),
(1000, 'サイケデリックトランス / Psyche Planet-V', 'HYPER', 41, 42),
(1237, 'ヴィジュアル４ / Desire', 'HYPER', 41, 42),
(1446, 'ギャラクシヴロック / Polaris', 'HYPER', 41, 42),
(2195, 'ドリームチャンプル / Dimensiva Vulnus', 'HYPER', 41, 42),
(2387, 'スペースレクイエム / Zirkfied', 'HYPER', 41, 42),
(2611, 'メイドメタル / ホーンテッド★メイドランチ', 'HYPER', 41, 42),
(2826, 'Peragro', 'HYPER', 41, 42),
(3214, 'Fate No.23', 'HYPER', 41, 42),
(3438, '混乱少女♥そふらんちゃん!!', 'HYPER', 41, 42),
(3810, 'Spangles', 'HYPER', 41, 42),
(3814, 'Spiral Clouds', 'HYPER', 41, 42),
(4350, '♥LOVE² シュガ→♥ (かめりあ&ななひら''s Over-Sweet-Dempa ♥LOVE² シュガ→♥な恋愛教室 Remix)', 'HYPER', 41, 42),
(4234, 'CARTOON☆RagHour', 'HYPER', 41, 42),
(439, 'ヒップロック２ / 男々道', 'HYPER', 42, 43),
(498, 'シンフォニックメタル / Holy Forest', 'HYPER', 42, 43),
(1987, '姫コア / KIMONO♥PRINCESS', 'HYPER', 42, 43),
(4890, 'Globe Glitter', 'HYPER', 42, 43),
(28, 'ヘビーメタル / I''m on Fire', 'HYPER', 43, 44),
(5002, 'Revived After Ruined Shine', 'HYPER', 43, 44),
(5921, 'クラシック１１ / 想い出をありがとう', 'HYPER', 45, 46),
(5832, 'ファニー / PULSE', 'EX', 39, 40),
(671, 'ダイナマイトソウル / １クールの男', 'EX', 39, 40),
(6232, '俺ポップ / 垂直OK!', 'EX', 40, 41),
(992, 'レイジャズ / NIGHT FEVER', 'EX', 40, 41),
(231, 'ミスティ / platonic love', 'EX', 41, 42),
(5852, 'クラシック５ / Step in Space', 'EX', 41, 42),
(579, 'ロマネスク / ラブ・アコーディオン', 'EX', 41, 42),
(858, 'チップトロニカ / Opportunity', 'EX', 41, 42),
(1056, '応援歌 / 燃やせ！青春 ～ポップン学園応援歌～', 'EX', 41, 42),
(1295, 'スムーズソウル / Runnin'' Away', 'EX', 41, 42),
(2087, 'ソナチネトロニカ / 時を止める魔女', 'EX', 41, 42),
(2126, 'テクノガールREMIX / 魔法的新定義 electro mix', 'EX', 41, 42),
(5819, 'タッキュウブギ / Ping Pong Boogie', 'EX', 42, 43),
(1848, 'カラオケREMIX / 愛言葉～アイコトバ～CYBER VIP ECHO MIX～', 'EX', 42, 43),
(750, 'ゴエモン / がんばれゴエモンメドレー', 'EX', 43, 44),
(1312, 'ジグREMIX / Tir na n''Og (Europa GT Remix)', 'EX', 43, 44),
(4231, 'Burning Love', 'EX', 44, 45),
(740, 'ハイパーロッケンローレ / エイプリルフールの唄', 'EX', 45, 46),
(582, '大河REMIX / ANAHONIKUY -雪の華PuzzLeMix-', 'EX', 45, 46),
(989, 'トライ▼ユーロ / Let''s go out!', 'EX', 45, 46),
(998, 'ポップデスコ / popdod', 'EX', 45, 46),
(1741, 'メカニカルジャズ / Apple Butter', 'EX', 45, 46),
(1787, 'モフロック / moffing', 'EX', 45, 46),
(319, 'ラウンジポップ / Linus', 'EX', 46, 47),
(6821, 'ONYX', 'EX', 47, 48),
(5495, 'Versa(UPPER)', 'EX', 48, 49);

CREATE TEMPORARY TABLE popn29_level_assertions (
  ok TINYINT NOT NULL CHECK (ok = 1)
);

-- Abort if a target is missing, has the wrong difficulty, or has an unknown level.
INSERT INTO popn29_level_assertions
SELECT IF(COUNT(*) = 0, 1, 0)
FROM popn29_level_updates u
LEFT JOIN charts c ON c.chart_id = u.chart_id
WHERE c.chart_id IS NULL
   OR BINARY c.difficulty_label <> BINARY u.difficulty_label
   OR c.level NOT IN (u.old_level, u.new_level);

UPDATE charts c
JOIN popn29_level_updates u ON u.chart_id = c.chart_id
SET c.level = u.new_level,
    c.updated_at = CURRENT_TIMESTAMP
WHERE c.level = u.old_level;

-- Verify all 91 desired values before committing.
INSERT INTO popn29_level_assertions
SELECT IF(COUNT(*) = 91 AND SUM(c.level = u.new_level) = 91, 1, 0)
FROM popn29_level_updates u
JOIN charts c ON c.chart_id = u.chart_id;

COMMIT;
