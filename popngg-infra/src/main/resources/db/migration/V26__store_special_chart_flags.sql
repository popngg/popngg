CREATE TABLE chart_special_flags (
    chart_id BIGINT NOT NULL PRIMARY KEY,
    has_strict_gauge BOOLEAN NOT NULL DEFAULT FALSE,
    has_strict_judgement BOOLEAN NOT NULL DEFAULT FALSE,
    extra_type VARCHAR(16) NOT NULL DEFAULT 'NONE',
    source VARCHAR(64) NOT NULL,
    as_of DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_chart_special_flags_extra_type (extra_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Curated from docs/data/popn_lv48_50_special_flags.json (as of 2026-09-25).
INSERT INTO chart_special_flags
    (chart_id, has_strict_gauge, source, as_of)
VALUES
    (783, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (1102, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (1339, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (1747, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2070, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2204, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2388, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2521, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2525, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (2803, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3339, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3415, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3431, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3579, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3631, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3683, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3699, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3759, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (3995, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4159, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4191, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4227, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4359, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4379, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4383, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4395, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4399, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4551, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4587, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4595, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4607, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4675, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4867, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5055, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5067, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5195, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5203, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5295, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5303, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5355, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5435, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5515, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5535, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5563, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5567, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5591, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5663, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5704, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6258, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6278, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6320, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6324, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6423, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6439, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6547, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6696, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6739, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6885, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (6961, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (7101, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (7133, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (7233, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (7297, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (7476, TRUE, 'popn_lv48_50_special_flags', '2026-09-25');

INSERT INTO chart_special_flags
    (chart_id, has_strict_judgement, source, as_of)
VALUES
    (29, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (113, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (454, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (488, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (777, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (4227, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5704, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5721, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5749, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5785, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5922, TRUE, 'popn_lv48_50_special_flags', '2026-09-25'),
    (5931, TRUE, 'popn_lv48_50_special_flags', '2026-09-25')
ON DUPLICATE KEY UPDATE
    has_strict_judgement = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO chart_special_flags
    (chart_id, extra_type, source, as_of)
VALUES
    (6865, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7205, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7225, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7317, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7389, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7480, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7492, 'EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7133, 'SUPER_EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7297, 'SUPER_EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7476, 'SUPER_EXTRA', 'popn_lv48_50_special_flags', '2026-09-25'),
    (7484, 'SUPER_EXTRA', 'popn_lv48_50_special_flags', '2026-09-25')
ON DUPLICATE KEY UPDATE
    extra_type = CASE
        WHEN chart_id IN (7133, 7297, 7476, 7484) THEN 'SUPER_EXTRA'
        ELSE 'EXTRA'
    END,
    updated_at = CURRENT_TIMESTAMP;
