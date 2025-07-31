CREATE DATABASE IF NOT EXISTS popnggdb
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE popnggdb;

CREATE TABLE chart
(
    difficulty INT          NULL,
    is_deleted INT          NULL,
    is_upper   INT          NULL,
    level      INT          NULL,
    version    INT          NULL,
    chart_id   BIGINT AUTO_INCREMENT
        PRIMARY KEY,
    genre_name VARCHAR(255) NULL,
    jacket     VARCHAR(255) NULL,
    song_hash  VARCHAR(255) NULL,
    song_name  VARCHAR(255) NULL,
    created_at DATETIME     NULL
) ENGINE = INNODB;


INSERT INTO chart VALUES (3, 0, 0, 49, 3, 1, 'TestGenreName1', 'TestJacket1', 'TestSongHash1', 'TestSongName1', '2023-06-01 00:00:00'),
                         (4, 0, 1, 49, 10, 2, 'TestGenreName2', 'TestJacket2', 'TestSongHash2', 'TestSongName2', '2023-06-01 00:00:00'),
                         (4, 1, 0, 49, 15, 3, 'TestGenreName3', 'TestJacket3', 'TestSongHash3', 'TestSongName3', '2023-06-01 00:00:00'),
                         (4, 0, 0, 50, 15, 4, 'TestGenreName4', 'TestJacket4', 'TestSongHash4', 'TestSongName4', '2023-06-01 00:00:00');
