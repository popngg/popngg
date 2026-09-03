package gg.popn.infra.db.adapter;

import org.springframework.jdbc.core.JdbcTemplate;

final class UserDirectoryTestSchema {
    static void create(JdbcTemplate jdbc) {
        jdbc.execute("DROP TABLE IF EXISTS user_clear_levels");
        jdbc.execute("DROP TABLE IF EXISTS user_directory_revision");
        jdbc.execute("CREATE TABLE user_clear_levels(user_id BIGINT NOT NULL, current_version INT NOT NULL, clear_level INT NOT NULL, PRIMARY KEY(user_id,current_version))");
        jdbc.execute("CREATE TABLE user_directory_revision(id INT PRIMARY KEY, revision BIGINT NOT NULL)");
        jdbc.update("INSERT INTO user_directory_revision VALUES (1, 1)");
    }
}
