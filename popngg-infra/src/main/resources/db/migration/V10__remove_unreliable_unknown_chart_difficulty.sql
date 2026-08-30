ALTER TABLE unknown_chart_reports
    DROP INDEX uk_unknown_chart_identity,
    MODIFY difficulty_code TINYINT NULL,
    MODIFY is_upper BOOLEAN NULL;

UPDATE unknown_chart_reports
SET difficulty_code = NULL, is_upper = NULL;

CREATE TEMPORARY TABLE unknown_chart_aggregate AS
SELECT MIN(report_id) AS report_id,
       SUM(occurrences) AS occurrences,
       MIN(first_seen_at) AS first_seen_at,
       MAX(last_seen_at) AS last_seen_at
FROM unknown_chart_reports
GROUP BY song_name, genre_name, artist_name;

UPDATE unknown_chart_reports report
JOIN unknown_chart_aggregate aggregate ON aggregate.report_id = report.report_id
SET report.occurrences = aggregate.occurrences,
    report.first_seen_at = aggregate.first_seen_at,
    report.last_seen_at = aggregate.last_seen_at;

DELETE duplicate
FROM unknown_chart_reports duplicate
JOIN unknown_chart_reports keeper
  ON duplicate.song_name = keeper.song_name
 AND duplicate.genre_name = keeper.genre_name
 AND duplicate.artist_name = keeper.artist_name
 AND duplicate.report_id > keeper.report_id;

ALTER TABLE unknown_chart_reports
    ADD UNIQUE KEY uk_unknown_song_identity (song_name, genre_name, artist_name);

DROP TEMPORARY TABLE unknown_chart_aggregate;
