-- Production song data was imported with MySQL 8's utf8mb4_0900_ai_ci while
-- unknown_chart_reports was created as utf8mb4_unicode_ci. MySQL refuses an
-- equality comparison between those implicit collations, which broke the
-- Discord unknown/incomplete report commands. Restore the catalog columns to
-- the collation declared by the baseline schema.
ALTER TABLE songs
    MODIFY song_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY genre_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY artist_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE unknown_chart_reports
    MODIFY song_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY genre_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY artist_name VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
