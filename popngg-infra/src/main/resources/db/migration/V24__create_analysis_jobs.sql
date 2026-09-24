CREATE TABLE analysis_job_guard (
    guard_id TINYINT PRIMARY KEY
) ENGINE=InnoDB;
INSERT INTO analysis_job_guard (guard_id) VALUES (1);

CREATE TABLE analysis_jobs (
    job_id VARCHAR(36) PRIMARY KEY,
    request_key VARCHAR(120) NOT NULL,
    trigger_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    active_slot TINYINT NULL,
    created_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    finished_at DATETIME(6) NULL,
    result_json MEDIUMTEXT NULL,
    notification_pending BOOLEAN NOT NULL DEFAULT FALSE,
    notification_attempts INT NOT NULL DEFAULT 0,
    notification_next_at DATETIME(6) NULL,
    UNIQUE KEY uk_analysis_request (request_key),
    UNIQUE KEY uk_analysis_active (active_slot),
    KEY idx_analysis_notification (notification_pending, notification_next_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE analysis_job_requests (
    request_key VARCHAR(120) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    KEY idx_analysis_request_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
