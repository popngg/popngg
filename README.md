# popn.gg server

Backend for [popn.gg](https://popn.gg), providing pop'n music chart and playdata information.

## Project structure

```text
popngg-api/          HTTP controllers, requests, responses, and runtime entry point
popngg-application/  Use cases, application services, and ports
popngg-domain/       Domain models and policies
popngg-infra/        Database, security, and external-system adapters
deploy/              Production Compose and deployment scripts
```

## Environments

Configuration is split by Spring profile. No production secret is committed.

| Profile | Purpose | Database | Secrets |
| --- | --- | --- | --- |
| `local` | Developer workstation | `compose.local.yml`, bound to `127.0.0.1` | Safe local defaults; environment variables may override them |
| `test` | Automated tests | H2 or Testcontainers | Test-only values supplied by tests |
| `prod` | Staging and production | External values through `SPRING_DATASOURCE_*` | Required environment variables or a secret manager |

The common `application.yml` contains only shared behavior and environment-variable
references. Never add a real password or JWT key to a tracked file.

## Local development

Prerequisites: JDK 21 and Docker with Docker Compose.

Spring Boot starts `compose.local.yml` automatically and Flyway creates the schema.

```bash
./gradlew :popngg-api:bootRun --args='--spring.profiles.active=local'
```

On Windows PowerShell:

```powershell
.\gradlew.bat :popngg-api:bootRun --args="--spring.profiles.active=local"
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Tests

```bash
./gradlew test
```

MySQL-specific integration tests run when Docker is available and are skipped otherwise.

## Production deployment

Copy `.env.example` to `.env`, replace every placeholder, and keep `.env` untracked.
Use a unique random JWT key of at least 64 characters. The production Compose stack uses
the `prod` profile and refuses to start when required values are absent. Set
`CORS_ALLOWED_ORIGINS` to the exact dev or production frontend origins. Deployment scripts
automatically load the root `.env` file.

```bash
cp .env.example .env
docker compose --env-file .env -f deploy/compose.yml config
./deploy/bin/build-image.sh
./deploy/bin/deploy.sh
```

The API binds to `127.0.0.1:${API_PORT}` and should be exposed through an HTTPS reverse
proxy. Cookie-authenticated frontend requests must set `credentials: include`.

In managed hosting, inject the variables from the platform's secret manager instead of
creating an `.env` file. See `deploy/README.md` for migration and rollback details.

## Monitoring

### Deployment version

Discord administrators can run `/배포버전` to inspect the API instance handling the command.
They can also use `/성능대시보드` for a live Prometheus summary, `/장애상태확인` for a
read-only threshold-based health classification, and `/에러알림테스트` to verify the Discord
API-error webhook and its trace-linked Loki view. Automatic outage alerts and Discord
incident threads are not created by these commands.
The monitoring Compose stack runs an independent incident-bot that creates and resolves
Discord threads when Prometheus thresholds remain unhealthy. Administrators can verify
that delivery path safely with `/장애알림테스트`.
The private reply includes the release version, full Git commit, image build time, and
server start time (KST). Existing Discord guild, role, and signature checks still apply.

`deploy/bin/build-image.sh` generates versions such as `2026.09.02.194605-60b34ec`
(image build date/time in Asia/Seoul plus the short Git SHA). Each image retains its
commit-SHA deployment tag and also receives the date-based tag. Metadata is baked into
the image and OCI labels, not read from the host checkout or `.env` at query time.
Rebuilding the same commit generates a new date-based version. This identifies a build,
not proof that deployment succeeded; the bot reports the image actually serving it.
Rollback to an image built with this feature reports that image's older version.
Versions predating this feature do not support the command. Direct Docker builds without
metadata report `local` / `unknown` rather than guessing a deployed revision.

No Discord portal change is required when guild command registration is already configured;
the command is registered on API startup. Do not override `POPNGG_RELEASE_VERSION`,
`POPNGG_GIT_SHA`, or `POPNGG_BUILD_TIME` in Compose or `.env`.

An optional Prometheus, Loki, Alloy, and Grafana stack collects Spring Boot metrics and
structured JSON logs. It is isolated from the API lifecycle and binds its administration
ports to host loopback only. See [deploy/monitoring/README.md](deploy/monitoring/README.md)
for setup, SSH access, dashboard definitions, retention, and troubleshooting.

## Technology

- Java 21
- Spring Boot 3.5
- Gradle 8.13 wrapper
- MySQL 8 and Flyway
- Docker Compose

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening
a pull request. Security vulnerabilities must be reported privately according
to [SECURITY.md](SECURITY.md), not through a public issue.

## License and content notice

The software source code and project documentation are licensed under the
[Apache License 2.0](LICENSE).

Game data, song and chart metadata, jacket artwork, third-party trademarks, and
other copyrighted game assets are not granted under the Apache License 2.0.
Their respective rights remain with their owners. Do not redistribute those
materials unless you have separate permission to do so.
