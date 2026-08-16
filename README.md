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
the `prod` profile and refuses to start when required values are absent.

```bash
cp .env.example .env
docker compose --env-file .env -f deploy/compose.yml config
./deploy/bin/build-image.sh
./deploy/bin/deploy.sh
```

In managed hosting, inject the variables from the platform's secret manager instead of
creating an `.env` file. See `deploy/README.md` for migration and rollback details.

## Technology

- Java 21
- Spring Boot 3.5
- Gradle 8.13 wrapper
- MySQL 8 and Flyway
- Docker Compose
