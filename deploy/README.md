# Docker deployment and smoke test

The API image is built and run with Java 21. Use a commit SHA or release tag; deployment
scripts reject `latest`. Secrets are supplied through the environment and are never stored
in this directory.

The container always uses the Spring `prod` profile. Docker is the packaging/runtime
mechanism, not the environment name. Local development uses the separate root-level
`compose.local.yml` and the `local` profile.

## Stages

1. Build `:popngg-api:bootJar` and the immutable API image.
2. Acquire the environment deployment lock.
3. Start MySQL 8 and wait for its health check.
4. Run Flyway as a one-shot migration service.
5. Run the idempotent pop'n 29 catalog migrations: confirmed deletions/renames,
   1,953 existing-song v3 metadata updates, then 108 new songs and 432 charts.
6. Start the API with application Flyway disabled.
7. Wait for the internal `/actuator/health`, then run public smoke tests against `/health`.

```bash
export IMAGE_REPOSITORY=popngg-api
export IMAGE_TAG="$(git rev-parse HEAD)"
export DB_PASSWORD='local-only'
export DB_ROOT_PASSWORD='local-root-only'
export JWT_SECRET_KEY='replace-with-at-least-64-random-characters-before-deploy'

./deploy/bin/build-image.sh
./deploy/bin/deploy.sh
```

Alternatively copy the repository root `.env.example` to `.env`, fill it in, and load it
in. `build-image.sh` and `deploy.sh` automatically load the root `.env`; set `ENV_FILE`
to use another path. `.env` is ignored by Git. Never reuse the local profile's
development JWT key in staging or production.

Set `CORS_ALLOWED_ORIGINS` to the exact comma-separated frontend origins, without trailing
slashes. The API port binds to `127.0.0.1` and is intended to run behind an HTTPS reverse
proxy. Login cookies are `Secure`, so browser cookie authentication does not work over
plain HTTP. Frontend requests must use `credentials: include`.

Production uses `https://popn.gg` for the Vercel frontend and `https://api.popn.gg` for
the Oracle-hosted API. Nginx terminates TLS and proxies to `127.0.0.1:8080`; Spring trusts
the forwarded request metadata through `SERVER_FORWARD_HEADERS_STRATEGY=framework`.
After DNS, Nginx, and Let's Encrypt are configured, set `BASE_URL=https://api.popn.gg` so
the deployment smoke test verifies the public HTTPS path rather than only loopback HTTP.

The Compose project or volume must be backed up before production migration. Rollback
means deploying the previously recorded immutable image tag; schema rollback requires an
explicit reviewed forward migration and is not performed automatically.

The catalog migration first marks 60 confirmed deleted charts. It then preserves existing
IDs and playdata while updating 1,953 existing songs to the v3 hash, official genre/artist
(including the 560 genre changes and `つぼみ` correction), and S3 jacket URL.
Finally, MySQL assigns new auto-increment `song_id` values to 108 High☆Cheers!! songs
(100 regular and 8 UPPER) and inserts their 432 charts. Each SQL file is idempotent and
uses transactional assertions; a failure prevents the API container from starting. After
all three data steps succeed, `catalog_data_migrations` records
`popn29-v3-catalog-20260821`. Later deployments skip this completed data migration, so
future catalog edits are not compared against the historical before-state on every merge.

## Smoke scenarios

After a successful candidate deployment, the automation creates or updates the open
`develop` to `main` release pull request with the deployed SHA. Complete the required
review and checks, then use **Create a merge commit**. A `main` branch ruleset rejects
squash and rebase for release PRs; feature pull requests into `develop` may still use
squash merging.

Every smoke request logs its label, HTTP status, time to first byte, total duration,
and curl exit code. The clear-level page is requested twice (first load and repeat);
neither request gets a relaxed timeout. The first request is not necessarily a cache
miss if public traffic already populated it. Bodies, passwords and tokens are not logged.

The default test verifies health, song search, user ranking, and a 20-user page sorted
by clear level (`/api/v1/users?sort=clearLevel&order=desc&page=1&size=20`). Each request
must finish within 10 seconds. A slow clear-level query fails the candidate deployment
check and can trigger the existing rollback workflow; this is a smoke-test limit, not
a production latency SLA. Set
`SMOKE_POPTOMO_ID` to also verify the public user profile and playdata query. Set both
`SMOKE_POPTOMO_ID` and `SMOKE_LOGIN_PASSWORD` only for a dedicated non-production smoke
account to verify login.

The deployment checklist must additionally exercise an authenticated playdata import in a
disposable staging account, then verify the affected user playdata and chart ranking. Do
not run a write smoke against a real user. Record only status codes, counts, image tag,
and timestamps; never record credentials or tokens.

## User directory cache

The production Compose stack starts an internal-only Redis cache (no published port).
It has a 128 MB eviction budget, a 192 MB container memory limit, five-minute response
TTLs, and no persistence. Cache data is disposable; MySQL remains the source of truth.
See [Redis eviction guidance](https://redis.io/docs/latest/develop/reference/eviction/).

Only the first page of public user lists without search terms and public rankings is
cached. Sort, direction, page size, game version, application instance and the committed
DB revision distinguish keys. Later pages, searches and individual/private profiles
bypass this cache. Renewal, recalculation, registration, profile/privacy changes and
catalog writes invalidate it transactionally. Results are filled on the next request,
not synchronously warmed during a renewal. Old revision keys expire naturally; no
Redis `KEYS` scan or broad `FLUSHALL` is used by the application.

V21 adds `user_clear_levels` and `user_directory_revision`. Clear levels are computed
on renewal/recalculation, with catalog changes rebuilding the summaries. The first
uncached list request joins the small summary table instead of aggregating all playdata.
Summary maintenance uses a shared revision-row lock to serialize directory-affecting
writes; imports acquire their existing user lock first to preserve the duplicate-key
fix. Concurrent renewals may wait for one another; monitor write latency if renewal
volume grows. Reads are not blocked by this lock. Catalog update uses READ COMMITTED
so a waited-for renewal's committed playdata is included in the rebuild.

At API startup summaries are reconciled once, covering catalog SQL migrations and
writes made by the older application during a rollback. The schema is additive. Keep
these tables when rolling back. Do not run mixed old/new API writers simultaneously
or make direct SQL catalog/playdata edits while serving traffic; perform such maintenance
with writes paused and restart the API afterward to reconcile and invalidate.

Redis connection/command timeouts are 200 ms. On cache failure the API falls back to DB
and backs off Redis access for 30 seconds. Redis is excluded from API health because
it is an optional cache; check its own Compose health separately. The DB revision makes
invalidation independent of Redis availability. To disable caching for a deployment,
set `USER_DIRECTORY_CACHE_ENABLED=false` in the deployment environment and recreate
the API; this retains summary-table optimization. See
[Spring Boot Redis properties](https://docs.spring.io/spring-boot/appendix/application-properties/index.html).

For local testing, `compose.local.yml` publishes Redis only at `127.0.0.1:6379`.
Set `POPNGG_USER_DIRECTORY_CACHE_ENABLED=true` for the locally running API (default off).
Before merging, run `./gradlew test` with Docker available: the suite includes MySQL
summary/rollback tests and a Redis TTL/eviction integration test. Without Docker those
container tests are explicitly skipped; mock/H2 tests alone do not establish live
deployment performance. In staging check a cold request, repeat request, renewal,
privacy change, Redis outage and recovery, then compare the smoke timings.

## Inspect the database locally

Adminer is available as an opt-in Compose tool after MySQL is running. It binds only to
the local loopback interface and is not exposed on the host's external network.

```bash
docker compose -f deploy/compose.yml --profile tools up -d adminer
```

Open `http://127.0.0.1:8081` and use:

- System: `MySQL`
- Server: `mysql`
- Username: `popngg`
- Database: `popngg`
- Password: the local `DB_PASSWORD` value

Set `ADMINER_PORT` to change the host port. Do not expose Adminer publicly or use it as a
production administration endpoint. Stop it without removing MySQL data:

```bash
docker compose -f deploy/compose.yml --profile tools stop adminer
```

For a local teardown:

```bash
docker compose -f deploy/compose.yml down --volumes
```
