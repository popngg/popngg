ALTER TABLE playdata
    ADD COLUMN version_score_known BOOLEAN NOT NULL DEFAULT FALSE
    AFTER version_score;
