ALTER TABLE playdata
    ADD COLUMN potential_popclass INT NOT NULL DEFAULT 0 AFTER popclass,
    ADD KEY idx_playdata_user_potential_popclass
        (user_id, potential_popclass DESC, all_time_score DESC);

-- Cache every chart's potential value from its all-time best, including charts
-- that are not selected in the 20 current / 40 old popclass target table.
UPDATE playdata p
JOIN charts c ON c.chart_id = p.chart_id
SET p.potential_popclass = CASE
    WHEN p.all_time_score < 50000 THEN 0
    ELSE FLOOR(
        FLOOR(
            FLOOR(
                c.level * (
                    3750 * c.level
                    + CASE
                        WHEN p.medal_code = 1 THEN 21250
                        WHEN p.medal_code IN (2, 3, 4) THEN 17500
                        WHEN p.medal_code IN (5, 6, 7) THEN 12500
                        WHEN p.medal_code = 11 THEN 6250
                        WHEN p.medal_code = 12 THEN 10000
                        ELSE 0
                      END
                    + p.all_time_score - 50000
                ) * 100000000 / 3881250
            ) * 6000 / 100000000
        ) * 1000 / 6000
    )
END
WHERE c.is_deleted = FALSE;
