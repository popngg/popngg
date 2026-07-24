CREATE TABLE playdata (
    playdata_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chart_id BIGINT NOT NULL,
    current_version INT NOT NULL,
    version_score INT NOT NULL DEFAULT 0,
    version_rank_code TINYINT NULL,
    all_time_score INT NOT NULL DEFAULT 0,
    all_time_score_version INT NOT NULL DEFAULT 0,
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
    KEY idx_playdata_user_popclass (
        user_id, current_version, popclass DESC, version_score DESC
    ),
    KEY idx_playdata_user_version_chart (user_id, current_version, chart_id),
    KEY idx_playdata_chart_version_score (
        chart_id, current_version, version_score DESC
    ),
    KEY idx_playdata_chart_all_time_score (chart_id, all_time_score DESC),
    KEY idx_playdata_chart_medal_score (chart_id, medal_code, version_score DESC),
    KEY idx_playdata_user_rank (user_id, current_version, version_rank_code),
    KEY idx_playdata_user_medal (user_id, medal_code),
    KEY idx_playdata_user_display_target (
        user_id, current_version, is_display_popclass_target,
        popclass_bucket, popclass_bucket_rank
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE playdata_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    created_at DATETIME NOT NULL,
    KEY idx_history_user_chart_version_created (
        user_id, chart_id, game_version, created_at DESC
    ),
    KEY idx_history_user_created (user_id, created_at DESC),
    KEY idx_history_chart_created (chart_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
