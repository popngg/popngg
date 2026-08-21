INSERT INTO game_version_transitions (
    from_version,
    to_version,
    score_policy,
    status,
    created_by,
    memo,
    applied_at,
    created_at,
    updated_at
) VALUES (
    28,
    29,
    'RESET',
    'APPROVED',
    NULL,
    'Approve Popn 28 to 29 score transition',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    score_policy = 'RESET',
    status = 'APPROVED',
    memo = 'Approve Popn 28 to 29 score transition',
    applied_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP;
