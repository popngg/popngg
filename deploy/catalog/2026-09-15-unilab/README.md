# UniLab genre refresh — 2026-09-15

Official announcement: https://p.eagate.573.jp/game/popn/popn29/

Official source: https://p.eagate.573.jp/game/popn/popn29/music/list.html?version=27

Compared all six official UniLab pages (144 rows) with 2,092 public catalog entries.
116 existing songs need a genre change; no registered UniLab songs had missing or
ambiguous title/artist matches. Version 26 was also checked (131 official rows) and
had no remaining genre changes. `changes.csv` is the complete before/after review.

`V22__refresh_unilab_genres.sql` applies these updates through the normal Flyway
deployment. The generated `migration.sql` is a review copy, not an additional
deployment step. Do not run catalog SQL directly against the serving application.
Deployment startup reconciles the existing user-directory cache as documented in
`deploy/README.md`.

Each update checks song ID, version, exact title, artist and old genre. A later
administrator edit is left untouched. Repeating the SQL makes no further changes.
Song IDs, chart IDs, scores, history, levels, song hashes and jacket URLs stay intact.
Existing hashes are deliberately retained as public URL identifiers for this genre
correction; they are not recalculated from the new metadata or used to relocate images.

After deployment, check all 116 new genres through the public API and renew affected
users' records again. Previously skipped input scores were not stored in the unknown
chart report, so this migration cannot replay those renewals. Clients sending old
genre strings without a chart ID still need to obtain a fresh official page.

Crimson Eyes was not present in the current catalog and is not a UniLab genre update.
Adding missing songs/charts is separate from this metadata-only migration.

To prepare a fresh review (read-only network requests, local output only):

```sh
python deploy/catalog/prepare_genre_refresh.py --version 27 --output build/genre-review
```

Review generated data before adding a new numbered Flyway migration. Never rewrite an
already deployed migration using newly fetched data.
