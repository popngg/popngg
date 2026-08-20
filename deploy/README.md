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

## Smoke scenarios

The default test verifies health, song search, and user ranking. Set
`SMOKE_POPTOMO_ID` to also verify the public user profile and playdata query. Set both
`SMOKE_POPTOMO_ID` and `SMOKE_LOGIN_PASSWORD` only for a dedicated non-production smoke
account to verify login.

The deployment checklist must additionally exercise an authenticated playdata import in a
disposable staging account, then verify the affected user playdata and chart ranking. Do
not run a write smoke against a real user. Record only status codes, counts, image tag,
and timestamps; never record credentials or tokens.

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
