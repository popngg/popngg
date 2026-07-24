CREATE DATABASE `__TARGET_DB__`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `__TARGET_DB__`;

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY,
  poptomo_id VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(255) NULL,
  email_verified_at DATETIME NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_users_poptomo_id (poptomo_id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE user_profiles (
  user_id BIGINT PRIMARY KEY,
  user_name VARCHAR(100) NOT NULL,
  character_name VARCHAR(100) NOT NULL DEFAULT '',
  comment VARCHAR(255) NOT NULL DEFAULT '',
  profile_image_url VARCHAR(512) NULL,
  is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
  display_popclass INT NOT NULL DEFAULT 0,
  potential_popclass INT NOT NULL DEFAULT 0,
  legacy_popclass INT NOT NULL DEFAULT 0,
  normal_credit INT NOT NULL DEFAULT 0,
  extra_credit INT NOT NULL DEFAULT 0,
  time_play_10_credit INT NOT NULL DEFAULT 0,
  time_play_16_credit INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE songs (
  song_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  song_hash VARCHAR(255) NULL,
  genre_name VARCHAR(255) NOT NULL,
  song_name VARCHAR(255) NOT NULL,
  artist_name VARCHAR(255) NULL,
  version INT NOT NULL,
  jacket_url VARCHAR(512) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_songs_hash (song_hash)
) ENGINE=InnoDB;

CREATE TABLE charts (
  chart_id BIGINT PRIMARY KEY,
  song_id BIGINT NOT NULL,
  difficulty_code TINYINT NOT NULL,
  difficulty_label VARCHAR(16) NOT NULL,
  level TINYINT NOT NULL,
  chart_version INT NOT NULL,
  has_strict_judgement BOOLEAN NOT NULL DEFAULT FALSE,
  has_strict_gauge BOOLEAN NOT NULL DEFAULT FALSE,
  is_upper BOOLEAN NOT NULL DEFAULT FALSE,
  is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_charts_song_difficulty_upper (song_id, difficulty_code, is_upper),
  KEY idx_charts_song (song_id)
) ENGINE=InnoDB;

CREATE TABLE playdata (
  playdata_id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  chart_id BIGINT NOT NULL,
  current_version INT NOT NULL,
  version_score INT NOT NULL,
  version_rank_code TINYINT NULL,
  all_time_score INT NOT NULL,
  all_time_score_version INT NOT NULL,
  all_time_rank_code TINYINT NULL,
  medal_code TINYINT NOT NULL,
  popclass INT NOT NULL DEFAULT 0,
  is_display_popclass_target BOOLEAN NOT NULL DEFAULT FALSE,
  popclass_bucket VARCHAR(20) NULL,
  popclass_bucket_rank INT NULL,
  last_played_at DATETIME NULL,
  last_renew_log_id BIGINT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_playdata_user_chart (user_id, chart_id),
  KEY idx_playdata_user_score (user_id, all_time_score DESC)
) ENGINE=InnoDB;

CREATE TABLE playdata_history (
  history_id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  chart_id BIGINT NOT NULL,
  game_version INT NOT NULL,
  previous_version_score INT NULL,
  version_score INT NULL,
  previous_all_time_score INT NULL,
  all_time_score INT NULL,
  previous_rank_code TINYINT NULL,
  rank_code TINYINT NULL,
  previous_medal_code TINYINT NULL,
  medal_code TINYINT NULL,
  popclass INT NULL,
  event_type VARCHAR(32) NOT NULL,
  renew_log_id BIGINT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE migration_user_map (
  old_user_id BIGINT PRIMARY KEY,
  new_user_id BIGINT NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE migration_song_map (
  old_chart_id BIGINT PRIMARY KEY,
  old_song_hash VARCHAR(255) NULL,
  new_song_id BIGINT NOT NULL,
  new_song_hash VARCHAR(255) NULL
) ENGINE=InnoDB;

CREATE TABLE migration_chart_map (
  old_chart_id BIGINT PRIMARY KEY,
  new_chart_id BIGINT NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE migration_playdata_map (
  old_playdata_id BIGINT PRIMARY KEY,
  new_playdata_id BIGINT NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE migration_failures (
  failure_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_table VARCHAR(32) NOT NULL,
  source_id BIGINT NULL,
  reason_code VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_migration_failures_reason (source_table, reason_code)
) ENGINE=InnoDB;

CREATE TABLE migration_verification_results (
  check_name VARCHAR(128) PRIMARY KEY,
  failure_count BIGINT NOT NULL,
  checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
