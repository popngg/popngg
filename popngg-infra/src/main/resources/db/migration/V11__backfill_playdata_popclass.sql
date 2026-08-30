-- playdata rows created before per-chart popclass calculation was introduced
-- retained the column default (0). Backfill them with the exact new-chart
-- calculation used by PopclassPolicy for the current pop'n 29 version score.
UPDATE playdata p
JOIN charts c ON c.chart_id = p.chart_id
SET p.popclass = CASE
    WHEN p.current_version <> 29
      OR p.version_score_known = FALSE
      OR p.version_score < 50000
    THEN 0
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
                    + p.version_score - 50000
                ) * 100000000 / 3881250
            ) * 6000 / 100000000
        ) * 1000 / 6000
    )
END
WHERE c.is_deleted = FALSE;
