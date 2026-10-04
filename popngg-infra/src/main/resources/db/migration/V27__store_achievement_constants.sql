CREATE TABLE achievement_constant_guards (
    axis VARCHAR(16) PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO achievement_constant_guards (axis) VALUES ('MEDAL'), ('RANK');

CREATE TABLE achievement_constant_snapshots (
    snapshot_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    source_snapshot_id VARCHAR(160) NOT NULL,
    axis VARCHAR(16) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    model_status VARCHAR(24) NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    imported_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active_axis VARCHAR(16) NULL,
    row_count INT NOT NULL,
    UNIQUE KEY uk_achievement_snapshot_source_axis (source_snapshot_id, axis),
    UNIQUE KEY uk_achievement_snapshot_active_axis (active_axis),
    KEY idx_achievement_snapshot_generated (axis, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE achievement_constants (
    snapshot_id BIGINT NOT NULL,
    chart_id BIGINT NOT NULL,
    target VARCHAR(32) NOT NULL,
    raw_difficulty DOUBLE NULL,
    constant_value DECIMAL(7,4) NULL,
    lower_bound DECIMAL(7,4) NULL,
    upper_bound DECIMAL(7,4) NULL,
    player_count INT NOT NULL,
    achieved_count INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    hold_reasons JSON NOT NULL,
    PRIMARY KEY (snapshot_id, chart_id, target),
    KEY idx_achievement_constants_chart (chart_id, snapshot_id),
    KEY idx_achievement_constants_status (snapshot_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
