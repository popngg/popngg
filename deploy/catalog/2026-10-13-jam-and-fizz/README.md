# Jam&Fizz / High☆Cheers!! catalog refresh — 2026-10-13

Compared the authenticated official lists in the user's browser: four pages / 95
Jam&Fizz songs and six pages / 131 High☆Cheers!! songs. The anonymous official
lists expose only 40 / 75 songs, so they cannot establish full coverage.

Official sources:
- https://p.eagate.573.jp/game/popn/popn29/music/list.html?version=28&sort=music&sort_type=up
- https://p.eagate.573.jp/game/popn/popn29/music/list.html?version=29&sort=music&sort_type=up

The reviewed correction contains:
- 65 version-28 genres and two version-29 genres (Neverland and charm♡you).
- 14 previously empty version-28 artist names, matched by unique exact title and UPPER status.
- TEKNO KEISER EX, chart 7548: 47 → 46. All 522 available version-29 charts were compared;
  this is the only level difference. Two songs have no EX chart in the official list.
- 196 official jackets: all 131 available version-29 songs plus the 65 version-28 genre changes.

The user supplied charm♡you's genre/artist from a Discord log. Its genre is
included with that provenance, while its levels are not inferred. Zt!ri△,
charm♡you and 祭囃子 do not appear in this account's official list. At the user's
request these three jackets are excluded from replacement and existing-image normalization.

## Metadata

`V29__refresh_jam_fizz_genres_and_high_cheers_levels.sql` is the deployment migration.
`migration.sql` is an identical review copy; do not execute both manually.
Each update guards the reviewed ID, version, exact title, artist and previous value.
Empty artist names are matched with `COALESCE` so both NULL and empty legacy values
are supported. Newer administrator edits are retained. Song/chart IDs, song hashes,
scores, play history, jacket URLs and creation timestamps remain unchanged by this SQL.
Existing hashes remain public URL identifiers and are deliberately not recomputed.

`changes.csv`, `artist-changes.csv`, `level-changes.csv` and `report.json` contain
the review. `authenticated-28.tsv` and `authenticated-29.tsv` contain only music
metadata read from the browser DOM, without account information or credentials.
Their transcription checksums were compared with the browser's complete extracted lists.
`user-confirmed-29.tsv` records the user-supplied correction.

Reproduce this review offline:

```sh
python deploy/catalog/prepare_october_refresh.py \
  --snapshot deploy/catalog/2026-10-13-jam-and-fizz \
  --output build/october-review
```

## Jackets and server resizing

The attached reference is **244 × 58** pixels. Downloaded official PNGs are 243/244 × 58.
`jackets-original/` retains the downloaded originals. `jackets/` contains the verified
244 × 58 files prepared with the same Java processor used by server uploads.
Nonmatching aspect ratios are fitted on a transparent canvas, preserving the whole
image without stretching or cropping. Standard-size PNG bytes remain unchanged.
The processor checks source byte size and dimensions before allocating the decoded raster.
Discord uploads, S3 uploads/replacements and jacket copies use this normalization.

`jacket-sources.json` records original URLs, dimensions and SHA-256 values.
`jacket-plan.json` records all 196 prepared replacements and their expected new URLs.
The maintenance command also checks each official file against the reviewed song hash.

Replacements use immutable URLs of the form
`<songHash>-jacket-<imageSha256>.png`; previous S3 objects remain available for rollback.
This avoids a one-year cached screenshot surviving under an unchanged URL.
Subsequent Discord jacket edits also publish a new immutable URL, and metadata edits
copy the current jacket URL rather than the original hash object.
A failed DB edit can leave an unreferenced immutable image; it does not remove or
overwrite an active jacket. Do not delete retained objects while URLs still reference them.

### Production application

Build the new API image with the usual deployment workflow. Applying V29 only changes
metadata; S3 objects and jacket URLs require the separate command below.
The command uses the normal Compose API environment and AWS credential chain. It defaults
to a read-only dry run; credentials are never printed. The same deployment lock prevents
concurrent deployment/maintenance. Reports preserve every previous URL for rollback.

```sh
# Review against the target DB/S3; no uploads or DB updates.
bash deploy/bin/refresh-jackets.sh

# In a deployment maintenance window, after metadata migrations and with API writes paused:
docker compose --env-file .env -f deploy/compose.yml stop api
bash deploy/bin/refresh-jackets.sh --apply
docker compose --env-file .env -f deploy/compose.yml up -d --no-deps --wait api
bash deploy/bin/smoke-test.sh
```

The apply command refuses to run while the Compose API service is running.
Ensure every writer to the target DB is paused, including writers on other hosts.
It publishes official replacements, normalizes other stored jackets with mismatched
dimensions, and updates each URL only if its reviewed current hash/URL still match.
On a conflict it stops; rerunning safely resumes because already-correct URLs are skipped.
API startup reconciles summaries and invalidates the directory cache as documented in
`deploy/README.md`. Renew/recalculate affected users after the TEKNO KEISER level change
so stored popclass values reflect the new level.

For a local/standalone runtime with the normal `SPRING_DATASOURCE_*` and AWS environment:

```sh
./gradlew :popngg-infra:maintainJackets \
  -PjacketSourceDirectory=/absolute/path/to/deploy/catalog/2026-10-13-jam-and-fizz/jackets \
  -PnormalizeExistingJackets=true
# Add -PapplyJacketChanges=true only for the maintenance-window apply step.
```

No production DB or S3 writes were performed during preparation. Unit tests and the
bootJar build passed. The MySQL migration integration test is present but is skipped
on this workstation because Docker is unavailable; run it on a Docker-enabled host
before deployment.
