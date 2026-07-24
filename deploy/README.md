# Docker deployment and smoke test

The API image is built and run with Java 21. Use a commit SHA or release tag; deployment
scripts reject `latest`. Secrets are supplied through the environment and are never stored
in this directory.

## Stages

1. Build `:popngg-api:bootJar` and the immutable API image.
2. Acquire the environment deployment lock.
3. Start MySQL 8 and wait for its health check.
4. Run Flyway as a one-shot migration service.
5. Start the API with application Flyway disabled.
6. Wait for `/actuator/health`, then run smoke tests.

```bash
export IMAGE_REPOSITORY=popngg-api
export IMAGE_TAG="$(git rev-parse HEAD)"
export DB_PASSWORD='local-only'
export DB_ROOT_PASSWORD='local-root-only'
export JWT_SECRET_KEY='replace-with-at-least-64-random-characters-before-deploy'

./deploy/bin/build-image.sh
./deploy/bin/deploy.sh
```

The Compose project or volume must be backed up before production migration. Rollback
means deploying the previously recorded immutable image tag; schema rollback requires an
explicit reviewed forward migration and is not performed automatically.

## Smoke scenarios

The default test verifies health, song search, and user ranking. Set
`SMOKE_POPTOMO_ID` to also verify the public user profile and playdata query. Set both
`SMOKE_POPTOMO_ID` and `SMOKE_LOGIN_PASSWORD` only for a dedicated non-production smoke
account to verify login.

The deployment checklist must additionally exercise an authenticated playdata import in a
disposable staging account, then verify the affected user playdata and chart ranking. Do
not run a write smoke against a real user. Record only status codes, counts, image tag,
and timestamps; never record credentials or tokens.

For a local teardown:

```bash
docker compose -f deploy/compose.yml down --volumes
```
