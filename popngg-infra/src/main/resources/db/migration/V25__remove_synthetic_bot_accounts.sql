-- Only explicitly marked synthetic BOT identities are removed. A normal user's
-- account is never selected solely because its ID resembles a bot ID.
CREATE TEMPORARY TABLE synthetic_bot_accounts (
    user_id BIGINT PRIMARY KEY,
    poptomo_id VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
);
INSERT INTO synthetic_bot_accounts (user_id,poptomo_id)
SELECT user_id,poptomo_id FROM users
WHERE role='BOT' AND poptomo_id REGEXP '^BOT-[0-9]+-[0-9]+$';

DELETE t FROM password_reset_tokens t JOIN synthetic_bot_accounts b ON b.user_id=t.user_id;
DELETE h FROM playdata_history h JOIN synthetic_bot_accounts b ON b.user_id=h.user_id;
DELETE p FROM playdata p JOIN synthetic_bot_accounts b ON b.user_id=p.user_id;
DELETE c FROM user_clear_levels c JOIN synthetic_bot_accounts b ON b.user_id=c.user_id;
DELETE l FROM login_logs l JOIN synthetic_bot_accounts b ON b.user_id=l.user_id OR b.poptomo_id=l.poptomo_id COLLATE utf8mb4_unicode_ci;
DELETE l FROM renew_logs l JOIN synthetic_bot_accounts b ON b.user_id=l.user_id OR b.poptomo_id=l.poptomo_id COLLATE utf8mb4_unicode_ci;
DELETE r FROM unknown_chart_reports r JOIN synthetic_bot_accounts b ON b.poptomo_id=r.poptomo_id COLLATE utf8mb4_unicode_ci;
UPDATE game_version_transitions t JOIN synthetic_bot_accounts b ON b.user_id=t.created_by SET t.created_by=NULL;
DELETE p FROM user_profiles p JOIN synthetic_bot_accounts b ON b.user_id=p.user_id;
DELETE u FROM users u JOIN synthetic_bot_accounts b ON b.user_id=u.user_id;
UPDATE user_directory_revision SET revision=revision+1 WHERE id=1 AND EXISTS (SELECT 1 FROM synthetic_bot_accounts);
DROP TEMPORARY TABLE synthetic_bot_accounts;
