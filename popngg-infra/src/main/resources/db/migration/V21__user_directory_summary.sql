CREATE TABLE user_clear_levels (
    user_id BIGINT NOT NULL,
    current_version INT NOT NULL,
    clear_level INT NOT NULL,
    PRIMARY KEY (user_id, current_version)
);

CREATE TABLE user_directory_revision (
    id INT PRIMARY KEY,
    revision BIGINT NOT NULL
);
INSERT INTO user_directory_revision (id, revision) VALUES (1, 1);

INSERT INTO user_clear_levels (user_id, current_version, clear_level)
SELECT pd.user_id, pd.current_version, MAX(c.level)
FROM playdata pd JOIN charts c ON c.chart_id = pd.chart_id
WHERE c.is_deleted = FALSE AND pd.medal_code IN (1,2,3,4,5,6,7,11,12)
GROUP BY pd.user_id, pd.current_version;
