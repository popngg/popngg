# Discord administration

The Discord administration integration accepts signed interactions at
`POST /api/v1/discord/interactions`. Only the configured guild and administrator role
may use the commands.

## Commands

- `/곡추가 자켓:<image> 추가일:<YYYY-MM-DD>` creates a song and its charts.
- `/곡수정 song_id:<id> [자켓:<image>] [추가일:<YYYY-MM-DD>]` updates a song without
  deleting omitted charts.
- `/곡조회 검색어:<text>` searches the catalog.
- `/미등록목록` shows recently unmatched renewal rows.

Chart input uses `N:30,H:42,EX:48`; prefix it with `UPPER` for Upper charts.
Creation and modification require a preview confirmation. Drafts expire after 15 minutes.

## Alerts

Catalog and administrator actions use `DISCORD_ADMIN_WEBHOOK_URL`. Unexpected API 5xx
errors use `DISCORD_ERROR_WEBHOOK_URL`; identical method, path, and exception combinations
are suppressed for five minutes. Request bodies, cookies, authorization values, and query
strings are never included in error notifications.

Jackets are converted to PNG and stored at
`s3://${AWS_S3_BUCKET}/${AWS_S3_JACKET_PREFIX}/{songHash}.png`. The database stores the
CloudFront URL. Existing objects are backed up before replacement.
