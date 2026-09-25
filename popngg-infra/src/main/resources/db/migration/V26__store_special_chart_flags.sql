ALTER TABLE songs
    ADD COLUMN extra_type VARCHAR(16) NOT NULL DEFAULT 'NONE' AFTER jacket_url;

-- Curated from docs/data/popn_lv48_50_special_flags.json (as of 2026-09-25).
-- These are chart properties, so they stay on charts rather than songs.
UPDATE charts
SET has_strict_gauge = TRUE
WHERE chart_id IN (
    783, 1102, 1339, 1747, 2070, 2204, 2388, 2521, 2525, 2803,
    3339, 3415, 3431, 3579, 3631, 3683, 3699, 3759, 3995, 4159,
    4191, 4227, 4359, 4379, 4383, 4395, 4399, 4551, 4587, 4595,
    4607, 4675, 4867, 5055, 5067, 5195, 5203, 5295, 5303, 5355,
    5435, 5515, 5535, 5563, 5567, 5591, 5663, 5704, 6258, 6278,
    6320, 6324, 6423, 6439, 6547, 6696, 6739, 6885, 6961, 7101,
    7133, 7233, 7297, 7476
);

UPDATE charts
SET has_strict_judgement = TRUE
WHERE chart_id IN (
    29, 113, 454, 488, 777, 4227, 5704, 5721, 5749, 5785, 5922, 5931
);

UPDATE songs
SET extra_type = 'EXTRA'
WHERE song_id IN (1962, 2046, 2051, 2074, 2092, 2114, 2117);

UPDATE songs
SET extra_type = 'SUPER_EXTRA'
WHERE song_id IN (2028, 2069, 2113, 2115);
