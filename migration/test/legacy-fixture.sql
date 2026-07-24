CREATE TABLE `user` (
  user_id BIGINT PRIMARY KEY, poptomo_id VARCHAR(32), password VARCHAR(255),
  role VARCHAR(20), user_name VARCHAR(64), `character` VARCHAR(128),
  comment VARCHAR(255), is_hidden BOOLEAN, popclass INT,
  created_at DATETIME, updated_at DATETIME
);
CREATE TABLE chart (
  chart_id BIGINT PRIMARY KEY, song_hash VARCHAR(255), genre_name VARCHAR(255),
  song_name VARCHAR(255), version INT, jacket VARCHAR(512), difficulty INT,
  level INT, is_upper BOOLEAN, is_deleted BOOLEAN, created_at DATETIME
);
CREATE TABLE playdata (
  playdata_id BIGINT PRIMARY KEY, user_id BIGINT, chart_id BIGINT,
  score INT, `rank` INT, medal INT
);
INSERT INTO `user` VALUES
  (1, 'test-1', 'test-only', 'USER', 'one', '', '', FALSE, 100, NOW(), NOW()),
  (2, 'test-2', 'test-only', 'USER', 'two', '', '', FALSE, 200, NOW(), NOW());
INSERT INTO chart VALUES
  (10, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre', 'song', 28, NULL, 4, 49, FALSE, FALSE, NOW()),
  (11, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'genre', 'song', 28, NULL, 3, 47, FALSE, FALSE, NOW());
INSERT INTO playdata VALUES
  (100, 1, 10, 90000, 3, 4),
  (101, 1, 10, 91000, 2, 3),
  (102, 2, 11, 88000, 4, 5);
