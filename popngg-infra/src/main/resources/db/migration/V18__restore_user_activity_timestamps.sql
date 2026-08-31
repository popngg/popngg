-- V16 rebuilt a derived potential cache but also assigned CURRENT_TIMESTAMP to
-- user_profiles.updated_at. The users page treats that field as the latest
-- renewal time, making every migrated user appear newly renewed.
--
-- V16 updated all affected profiles in one statement, so they share one exact
-- timestamp. Find that timestamp close to Flyway's V16 installation time and
-- repair only rows that still carry it. Real renewals/profile edits performed
-- after deployment therefore remain untouched.
CREATE TEMPORARY TABLE v16_profile_timestamp AS
SELECT profile.updated_at AS corrupted_at
  FROM user_profiles profile
  JOIN playdata p ON p.user_id = profile.user_id
  JOIN flyway_schema_history history ON history.version = '16'
 WHERE p.current_version = 29
   AND p.version_score_known = FALSE
   AND p.last_renew_log_id IS NULL
   AND profile.updated_at BETWEEN history.installed_on - INTERVAL 1 MINUTE
                              AND history.installed_on + INTERVAL 1 HOUR
 GROUP BY profile.updated_at
 ORDER BY COUNT(DISTINCT profile.user_id) DESC
 LIMIT 1;

UPDATE user_profiles profile
JOIN users user_account ON user_account.user_id = profile.user_id
JOIN v16_profile_timestamp corrupted
  ON corrupted.corrupted_at = profile.updated_at
LEFT JOIN (
    SELECT user_id, MAX(created_at) AS renewed_at
      FROM renew_logs
     WHERE status IN ('SUCCESS', 'PARTIAL_SUCCESS')
       AND user_id IS NOT NULL
     GROUP BY user_id
) latest_renewal ON latest_renewal.user_id = profile.user_id
   SET profile.updated_at = COALESCE(
           latest_renewal.renewed_at,
           user_account.updated_at,
           profile.created_at
       );

DROP TEMPORARY TABLE v16_profile_timestamp;
