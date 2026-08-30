-- Legacy bulk migration stored imported rows as pop'n 28 current state. The API
-- serves the configured pop'n 29 state, which made those all-time records
-- invisible until each user renewed their data.
--
-- Preserve all-time score/rank/medal data, but do not claim that a pop'n 28
-- score is a known pop'n 29 version-best score. Rows already renewed on pop'n
-- 29 are deliberately excluded.
UPDATE playdata
SET current_version = 29,
    version_score = 0,
    version_score_known = FALSE,
    version_rank_code = NULL,
    popclass = 0,
    is_display_popclass_target = FALSE,
    popclass_bucket = NULL,
    popclass_bucket_rank = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE current_version = 28;
