-- Manual rollback for only this pilot's tags. Does not delete audit history.
-- Replace __TARGET_DB__ with the reviewed database name.
SET SESSION sql_mode = 'STRICT_ALL_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';
CREATE TEMPORARY TABLE pilot_rollback_assertion (ok INT NOT NULL);
INSERT INTO pilot_rollback_assertion (ok)
SELECT IF(COUNT(*) = 17 AND SUM(is_active) = 17
          AND COUNT(DISTINCT song_id) = 10, 1, NULL)
  FROM `__TARGET_DB__`.song_search_tags
 WHERE source = 'PILOT_20260927';
START TRANSACTION;
UPDATE `__TARGET_DB__`.song_search_tags
   SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP
 WHERE source = 'PILOT_20260927' AND is_active = TRUE;
SET @pilot_deactivated = ROW_COUNT();
INSERT INTO pilot_rollback_assertion (ok)
SELECT IF(@pilot_deactivated = 17, 1, NULL);
COMMIT;
SELECT 'PILOT_TAGS_DEACTIVATED' AS result, @pilot_deactivated AS tag_count;
