# Discord administration

The Discord administration integration accepts signed interactions at
`POST /api/v1/discord/interactions`. Only the configured guild and administrator role
may use the commands.

## Commands

- `/곡추가` accepts jacket, date, song/genre/artist/version, optional L/N/H/EX levels,
  and an `o`/`x` UPPER option in one slash command. Submission shows a JSON preview.
- `/곡수정` accepts `song_id` plus optional jacket, date, metadata, L/N/H/EX levels,
  and UPPER in one slash command. Omitted values and charts remain unchanged and a JSON
  preview is shown before confirmation.
- `/곡조회 검색어:<text>` searches the catalog.
- `/미등록목록` shows recently unmatched renewal rows. Selecting a row opens the same
  creation form with song, genre, artist, and UPPER prefilled.

Chart input uses `N:30,H:42,EX:48`; prefix it with `UPPER` for Upper charts.
Creation and modification require a preview confirmation. Drafts expire after 15 minutes.

## Alerts

Catalog and administrator actions use `DISCORD_ADMIN_WEBHOOK_URL`. Unexpected API 5xx
errors use `DISCORD_ERROR_WEBHOOK_URL`; identical method, path, and exception combinations
are suppressed for five minutes. Request bodies, cookies, authorization values, and query
strings are never included in error notifications.

Command responses are public in the Discord channel. Unknown-song reports only display
metadata available from the renewal page; they do not claim a difficulty or UPPER state.
GitHub PR, merge, CI, and deployment notifications require an Actions repository secret
named `DISCORD_ADMIN_WEBHOOK_URL` containing the administrator-channel webhook URL.

Jackets are converted to PNG and stored at
`s3://${AWS_S3_BUCKET}/${AWS_S3_JACKET_PREFIX}/{songHash}.png`. The database stores the
CloudFront URL. Existing objects are backed up before replacement.
