-- Align stored medal codes with the High Cheers source order:
-- A-J regular medals, K easy clear, L long-pop-off clear.
UPDATE playdata
   SET medal_code = CASE medal_code
       WHEN 8 THEN 11
       WHEN 9 THEN 8
       WHEN 10 THEN 9
       WHEN 11 THEN 10
       WHEN 0 THEN 13
       ELSE medal_code
   END
 WHERE medal_code IN (0, 8, 9, 10, 11);

UPDATE playdata_history
   SET previous_medal_code = CASE previous_medal_code
           WHEN 8 THEN 11
           WHEN 9 THEN 8
           WHEN 10 THEN 9
           WHEN 11 THEN 10
           WHEN 0 THEN 13
           ELSE previous_medal_code
       END,
       medal_code = CASE medal_code
           WHEN 8 THEN 11
           WHEN 9 THEN 8
           WHEN 10 THEN 9
           WHEN 11 THEN 10
           WHEN 0 THEN 13
           ELSE medal_code
       END
 WHERE previous_medal_code IN (0, 8, 9, 10, 11)
    OR medal_code IN (0, 8, 9, 10, 11);
