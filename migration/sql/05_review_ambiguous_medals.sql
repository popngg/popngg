-- Contains user and chart identifiers. Save only to a restricted file outside Git.
SELECT u.poptomo_id, a.user_id, a.chart_id, s.song_hash,
       c.difficulty_code, a.source_score, a.current_medal,
       h.last_history_medal, h.last_history_score
  FROM code12_audit a
  JOIN `__TARGET_DB__`.users u ON u.user_id = a.user_id
  JOIN `__TARGET_DB__`.charts c ON c.chart_id = a.chart_id
  JOIN `__TARGET_DB__`.songs s ON s.song_id = c.song_id
  LEFT JOIN code12_review_history h ON h.old_playdata_id = a.old_playdata_id
 WHERE a.audit_status = 'REVIEW'
 ORDER BY a.user_id, a.chart_id;
