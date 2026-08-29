CREATE TABLE unknown_chart_reports (
    report_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    renew_log_id BIGINT NOT NULL,
    poptomo_id VARCHAR(64) NOT NULL,
    song_name VARCHAR(255) NOT NULL,
    genre_name VARCHAR(255) NOT NULL,
    artist_name VARCHAR(255) NOT NULL DEFAULT '',
    difficulty_code TINYINT NOT NULL,
    is_upper BOOLEAN NOT NULL,
    occurrences INT NOT NULL DEFAULT 1,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    UNIQUE KEY uk_unknown_chart_identity (song_name, genre_name, artist_name, difficulty_code, is_upper),
    KEY idx_unknown_chart_unresolved_seen (resolved, last_seen_at)
);
