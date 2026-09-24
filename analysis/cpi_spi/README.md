# CPI / SPI descriptive analysis

This implements the data collection stage, not a final rating formula. All levels
are inspected; level tables allow 47–50 comparison. Charts below 50 users remain
in the descriptive output and carry independent CPI/SPI eligibility counts.

## Run in the application

1. Apply Flyway V24 (job queue and notification outbox only; no play records change).
2. Set `ANALYSIS_ENABLED=true`, `ANALYSIS_S3_BUCKET` to a **private** dedicated bucket,
   and the existing `DISCORD_ADMIN_WEBHOOK_URL` / Discord command credentials.
3. Give the application role `s3:GetBucketPublicAccessBlock` on that bucket and
   `s3:PutObject` on `cpi-spi/*` (or the configured prefix). All four bucket public
   access blocks must be enabled. Objects use AES256 server-side encryption.
4. Invoke `/실력분석최신화` with the configured Discord admin role, or POST to
   `/api/v1/admin/analysis/cpi-spi` with ADMIN authentication and optional
   `Idempotency-Key`. HTTP returns 202; Discord returns type 4 immediately, never a
   deferred type 5/loading response. No long-lived interaction token is stored.
5. GET `/api/v1/admin/analysis/cpi-spi/{jobId}` to inspect durable status.

The same queue is scheduled at 06:00 Asia/Seoul using Spring scheduling. The default
cron is `0 0 6 * * *`, overridable with `ANALYSIS_CRON`. The present research job
includes all levels, including 40+; a final production rating level range has not
been chosen. A server that is stopped at 06:00 does not backfill missed days; use
the manual command after recovery.

Concurrent requests coalesce into one active job. Every request key points to that
job even after completion. A MySQL session lock serializes workers across replicas.
A RUNNING job left by a crashed process is rebuilt using a fresh attempt ID after
the new worker acquires the lock. DB source extraction is a repeatable-read,
read-only transaction; only job orchestration tables are written.

S3 layout:

```
cpi-spi/snapshots/<jobId>/<attemptId>/records.jsonl
cpi-spi/snapshots/<jobId>/<attemptId>/records.csv
cpi-spi/snapshots/<jobId>/<attemptId>/catalog.json
cpi-spi/snapshots/<jobId>/<attemptId>/users.json
cpi-spi/snapshots/<jobId>/<attemptId>/source_metadata.json
cpi-spi/snapshots/<jobId>/<attemptId>/chart_stats.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/level_stats.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/user_stats.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/user_level_stats.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/score_percentiles.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/cpi_readiness.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/record_cohorts.{csv,json}
cpi-spi/snapshots/<jobId>/<attemptId>/data_quality.json
cpi-spi/snapshots/<jobId>/<attemptId>/summary.json
cpi-spi/snapshots/<jobId>/<attemptId>/report.md
cpi-spi/snapshots/<jobId>/<attemptId>/manifest.json
cpi-spi/latest.json
```

All files, including per-user records, are private. No public frontend endpoint
exposes them. Raw audit records include excluded rows with internal IDs and flags;
email, password hashes, names, tokens and comments are never selected. The public
population statistics exclude BOTs, hidden/missing profiles, deleted/orphan charts,
invalid levels and duplicate user/chart keys.

Manifest is uploaded last, with file hashes and source metadata. Only successful
analysis and uploads replace latest. Failed partial attempts may remain in S3 but
are not published. Configure S3 lifecycle retention and local output retention for
the deployment; raw snapshots can be large. Local files default to the ignored
`build/analysis/cpi-spi/` directory. The raw record limit defaults to 3,000,000;
The deployment container uses `/tmp/popngg-analysis` because `/app` is owned by
root and the application runs as UID 10001. Successfully uploaded snapshots are
durable in S3; local temporary files do not survive container replacement.
exceeding it fails explicitly rather than truncating data. Memory use includes
per-group integer score arrays and membership sets; size the worker heap using an
actual snapshot before enabling scheduled runs on the API host.

Completion is a JSON file sent through the existing admin webhook as `admin bot`.
Success/failure is stored before delivery. Non-2xx responses and network failures
retain notification_pending and retry with bounded exponential backoff. Delivery
is at least once: a timeout after Discord accepts a message can yield duplicates;
consumers should deduplicate by jobId. Restarting does not lose pending notifications.
Analysis is not rerun because notification delivery failed.

## Offline reproduction

Download the private snapshot into a local directory. With Java 21:

```
./gradlew :popngg-application:analyzeCpiSpi -PsnapshotDir=/absolute/snapshot/directory
```

Inputs are `records.jsonl`, `catalog.json`, `users.json`. This command writes local
statistics only, does not connect to the DB, upload to S3, or notify Discord. Derived
eligibility fields in raw JSON are recomputed by the policy to avoid trusting stale
flags. Reproduction must use the policy/build recorded in manifest.

## Definitions

- Record = one stored user/chart best, not one play attempt. `all_time_score` may
  be replaced by the source during approved version RESET transitions.
- CPI: medals 1–7 true, 8–10 false. 11 EASY, 12 LONGOFF, 13 NONE and unknown null.
  Score is not an eligibility condition for CPI.
- SPI: valid score 0–100000 with play evidence; a positive score is sufficient even
  without a medal. Zero with played medal 1–12 is included; zero with NONE/unknown
  is ambiguous and excluded. CLEAR is not an eligibility condition for SPI.
- clearRate denominator = CPI eligible record count. playerCount is distinct users;
  these are different at level scope. Scores use SPI eligible records only.
- medal rates use medalObservationCount and include NONE/unknown as separate
  categories. bad20OrLess means bronze medals 5/6; FC/PERFECT remain separate.
- Quantiles linearly interpolate position `(n-1)*q`; zero samples yield null.
  Empty CSV tables contain no rows; their JSON form is `[]`.
- ECDF = count(score <= S)/n. topInclusiveRate = count(score >= S)/n. Ties mean
  these values are not complementary. A small-sample P99 is not a reliable tail model.
- cpi_readiness uses a deterministic disjoint reference/validation chart split,
  reference clear-rate bins within the same official level, >=5 reference records,
  and Wilson intervals. It is exploratory, not a CPI rating. Users can still have
  different reference songs, so selection bias remains; this is not a matched
  common-chart experiment. No monotonic correction or heuristic bonuses are applied.
- lastPlayedAt may be absent. recordUpdatedAt is a DB write time, and migrations
  have rewritten dates/current_version. No inference of current skill or exact
  record age is justified from these fields alone. History is not an attempt log.

## Verification

```
./gradlew :popngg-application:test --tests 'gg.popn.application.analysis.*' \
  :popngg-infra:test --tests 'gg.popn.infra.analysis.*' --tests 'gg.popn.infra.db.adapter.AnalysisMySqlIntegrationTest' \
  :popngg-api:test --tests 'gg.popn.http.analysis.*' --tests 'gg.popn.http.discord.*'
```

MySQL integration tests use the repository Testcontainers setup and are skipped
when Docker is unavailable. H2 tests exercise queue state/idempotency but do not
replace MySQL streaming/snapshot verification. Live DB, real S3 and real Discord
delivery require configured credentials and are not proven by mocks.

A deterministic local smoke fixture is available:

```
python analysis/cpi_spi/generate_fixture.py --output build/analysis/cpi-spi/synthetic
./gradlew :popngg-application:analyzeCpiSpi -PsnapshotDir=/absolute/path/to/build/analysis/cpi-spi/synthetic
```

The generated directory is marked FIXTURE_NOT_PRODUCTION. These statistics are not
real user counts, and this smoke test does not upload or send anything externally.

## Local verification record (2026-09-23)

- Java 21 build and targeted tests: 35 passed, 1 MySQL/Testcontainers test skipped
  because Docker is not installed in this workspace.
- Synthetic end-to-end CLI: 60 fictitious users, 48 charts, 2,380 records produced
  CSV/JSON/report outputs. These are intentionally not reported as real population sizes.
- Live population counts, operational schema state, S3 upload and Discord delivery
  remain unverified until DB access and a private analysis bucket are configured.
