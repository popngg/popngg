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
