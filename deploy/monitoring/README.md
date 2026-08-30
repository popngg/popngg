# POPN.GG monitoring operations

The monitoring stack is optional infrastructure. The API does not depend on Prometheus or
Grafana, so either monitoring container can fail or restart without stopping POPN.GG.

## Architecture and security

```text
Spring Boot API :9091/actuator/prometheus (Compose network only)
                         |
                         v  scrape every 30 seconds
                  Prometheus :9090
                         |
                         v  provisioned datasource
                    Grafana :3000
```

The API's main port remains bound to host loopback for Nginx. Only the detail-free
`/actuator/health` endpoint remains on that port. The complete Actuator surface, including
Prometheus, listens on container port 9091 and is not published to the host. Prometheus and
Grafana bind their host ports to `127.0.0.1`; do not add them to Nginx or change those binds
to `0.0.0.0` without adding a separately reviewed authentication layer.

Prometheus labels use Micrometer's normalized MVC `uri` template, such as
`/api/v1/users/{id}`. No user, song, chart, poptomo, or trace identifier is added as a label.
Unmatched routes can appear as Micrometer's low-cardinality `UNKNOWN` or `NOT_FOUND` value.

## Configure and start

Set these values in the untracked repository-root `.env` file:

```dotenv
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=a-long-unique-random-password
```

Build/deploy the API normally, then start the optional monitoring services with the same
Compose project and environment file:

```bash
docker compose --env-file .env \
  -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml \
  up -d prometheus grafana
```

This command is also suitable for a local full-stack test after building the API image and
filling the normal deployment variables in `.env`. It does not modify `compose.local.yml`.
To stop monitoring without touching the API or database:

```bash
docker compose --env-file .env \
  -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml \
  stop prometheus grafana
```

Do not use `down --volumes` in production: it deletes monitoring history (and, when the base
Compose file is included, can delete database data). The named `prometheus-data` and
`grafana-data` volumes preserve state across ordinary restarts.

## Access Grafana and Prometheus

From the server itself, Grafana is at `http://127.0.0.1:3000`. From an administrator's
computer, use an SSH tunnel rather than exposing the port:

```bash
ssh -L 3000:127.0.0.1:3000 ubuntu@your-server
```

Then open `http://127.0.0.1:3000` locally and sign in with `GRAFANA_ADMIN_USER` and
`GRAFANA_ADMIN_PASSWORD`. Anonymous access and self-service signup are disabled. The
provisioned dashboard is in **Dashboards > POPN.GG > POPN.GG Production Overview**.

Prometheus is available to administrators through a similar optional tunnel:

```bash
ssh -L 9090:127.0.0.1:9090 ubuntu@your-server
```

Open `http://127.0.0.1:9090/targets` and confirm that `popngg-api` is `UP`. Prometheus has
no built-in login, so never expose this tunnel to other network interfaces.

## What the dashboard means

- **Request Rate** is the number of HTTP requests handled per second.
- **AVG** is total request duration divided by request count in the selected rate window.
- **P95** is the duration below which 95% of requests completed; the slowest 5% took longer.
- **P99** is the duration below which 99% completed and highlights tail latency.
- **5xx Rate** is the proportion of requests that returned a server error. The endpoint
  table also shows 4xx and 5xx counts over the selected dashboard time range.

The HTTP section contains overall rate and latency, status distribution, and a URI-template
endpoint table. JVM panels cover heap/non-heap, heap ratio, GC, threads, process CPU, and
system CPU. HikariCP panels cover active, idle, pending, and maximum connections plus
average/maximum acquisition time when exported by the active Hikari version.

## Checks and troubleshooting

Container and target checks:

```bash
docker compose --env-file .env -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml ps
docker compose --env-file .env -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml logs --tail=100 prometheus grafana
docker compose --env-file .env -f deploy/compose.yml exec prometheus \
  wget -qO- http://api:9091/actuator/prometheus | head
curl -fsS http://127.0.0.1:9090/api/v1/targets
curl -fsS http://127.0.0.1:3000/api/health
```

The internal management port is fixed at 9091 by the production Compose files so the
scrape target and API healthcheck cannot drift apart. Look for
`http_server_requests_seconds_bucket`, `jvm_memory_used_bytes`, and
`hikaricp_connections_active` in the scrape output. Histogram buckets appear only after the
API handles requests. Hikari acquisition metrics can likewise remain absent until the pool
has acquired connections.

Disk and retention checks:

```bash
docker system df -v
docker volume inspect popngg_prometheus-data popngg_grafana-data
curl -fsS http://127.0.0.1:9090/api/v1/status/flags
```

Prometheus keeps at most seven days of blocks and targets 1GB of retained blocks. WAL and
head chunks are not immediately removable, so actual volume use can temporarily exceed the
retention size. Monitor host disk capacity separately.

If the target is `DOWN`, confirm the API is healthy, both services use the same Compose
project, and the target is exactly `api:9091`. If Grafana has no
dashboard or datasource, inspect its logs for provisioning errors and verify the read-only
mounts under `/etc/grafana/provisioning` and `/var/lib/grafana/dashboards`. If only a panel
is empty, query its metric directly in Prometheus; a metric may require traffic or DB pool
activity before it exists.
