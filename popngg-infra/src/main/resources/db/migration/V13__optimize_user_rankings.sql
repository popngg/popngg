-- Ranking pages filter hidden profiles and sort by one popclass column.
-- Composite indexes let MySQL stop after the requested page instead of sorting all users.
CREATE INDEX idx_user_profiles_hidden_potential_popclass
    ON user_profiles (is_hidden, potential_popclass DESC, user_id);

CREATE INDEX idx_user_profiles_hidden_legacy_popclass
    ON user_profiles (is_hidden, legacy_popclass DESC, user_id);

