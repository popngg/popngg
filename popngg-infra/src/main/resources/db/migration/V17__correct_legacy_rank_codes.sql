-- The legacy dump used the compact eight-grade table:
-- 1=S, 2=AAA, 3=AA, 4=A, 5=B, 6=C, 7=D, 8=E.
-- The current High Cheers table inserted the plus grades between them, so
-- copying those numeric values made legacy S appear as S+, AA as AAA, etc.
-- V12 marks imported rows as version-score unknown. Requiring no renewal log
-- additionally protects rows that have since received actual version 29 data.
UPDATE playdata
   SET all_time_rank_code = CASE all_time_rank_code
           WHEN 1 THEN 2
           WHEN 2 THEN 3
           WHEN 3 THEN 5
           WHEN 4 THEN 7
           WHEN 5 THEN 9
           WHEN 6 THEN 10
           WHEN 7 THEN 11
           WHEN 8 THEN 12
           WHEN 0 THEN 13
           ELSE all_time_rank_code
       END,
       version_rank_code = CASE version_rank_code
           WHEN 1 THEN 2
           WHEN 2 THEN 3
           WHEN 3 THEN 5
           WHEN 4 THEN 7
           WHEN 5 THEN 9
           WHEN 6 THEN 10
           WHEN 7 THEN 11
           WHEN 8 THEN 12
           WHEN 0 THEN 13
           ELSE version_rank_code
       END,
       updated_at = CURRENT_TIMESTAMP
 WHERE current_version = 29
   AND version_score_known = FALSE
   AND last_renew_log_id IS NULL
   AND (all_time_rank_code BETWEEN 0 AND 8
        OR version_rank_code BETWEEN 0 AND 8);

UPDATE playdata_history
   SET previous_rank_code = CASE previous_rank_code
           WHEN 1 THEN 2 WHEN 2 THEN 3 WHEN 3 THEN 5 WHEN 4 THEN 7
           WHEN 5 THEN 9 WHEN 6 THEN 10 WHEN 7 THEN 11 WHEN 8 THEN 12
           WHEN 0 THEN 13 ELSE previous_rank_code
       END,
       rank_code = CASE rank_code
           WHEN 1 THEN 2 WHEN 2 THEN 3 WHEN 3 THEN 5 WHEN 4 THEN 7
           WHEN 5 THEN 9 WHEN 6 THEN 10 WHEN 7 THEN 11 WHEN 8 THEN 12
           WHEN 0 THEN 13 ELSE rank_code
       END
 WHERE game_version = 28
   AND event_type = 'MIGRATION'
   AND (previous_rank_code BETWEEN 0 AND 8 OR rank_code BETWEEN 0 AND 8);
