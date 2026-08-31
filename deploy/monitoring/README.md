# POPN.GG monitoring operations

The monitoring stack is optional infrastructure. The API does not depend on Prometheus,
Loki, Alloy, or Grafana, so a monitoring container can fail or restart without stopping
POPN.GG.

## Architecture and security

```text
Spring Boot API :9091/actuator/prometheus (Compose network only)
                         |
                         v  scrape every 30 seconds
                  Prometheus :9090
                         |
                         v  provisioned datasource
                    Grafana :3000 (host loopback)
                         ^
                         | Nginx HTTPS reverse proxy
                  grafana.popn.gg

Spring Boot JSON file log -> Alloy -> Loki -> Grafana Explore
```

The API's main port remains bound to host loopback for Nginx. Only the detail-free
`/health` endpoint remains on that port. The complete Actuator surface, including
Prometheus, listens on container port 9091 and is not published to the host. Prometheus,
Loki, and Grafana bind their host ports to `127.0.0.1`. Only Grafana is exposed through the reviewed
Nginx virtual host; Prometheus must never be added to Nginx or bound to `0.0.0.0`.

Prometheus labels use Micrometer's normalized MVC `uri` template, such as
`/api/v1/users/{id}`. No user, song, chart, poptomo, or trace identifier is added as a label.
Unmatched routes can appear as Micrometer's low-cardinality `UNKNOWN` or `NOT_FOUND` value.

## Configure and start

Set these values in the untracked repository-root `.env` file:

```dotenv
PROMETHEUS_PORT=9090
LOKI_PORT=3100
GRAFANA_PORT=3000
GRAFANA_DOMAIN=grafana.popn.gg
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=a-long-unique-random-password
```

When `GRAFANA_ADMIN_PASSWORD` is set, the normal deployment script starts the optional
monitoring services after the API passes its smoke tests. A monitoring startup failure is
reported as a warning and does not fail or roll back the healthy API deployment:

```bash
./deploy/bin/deploy.sh
```

To start or repair only the monitoring services manually, use the same Compose project and
environment file:

```bash
docker compose --env-file .env \
  -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml \
  up -d --wait prometheus loki alloy grafana
```

This command is also suitable for a local full-stack test after building the API image and
filling the normal deployment variables in `.env`. It does not modify `compose.local.yml`.
To stop monitoring without touching the API or database:

```bash
docker compose --env-file .env \
  -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml \
  stop prometheus loki alloy grafana
```

Do not use `down --volumes` in production: it deletes monitoring history (and, when the base
Compose file is included, can delete database data). The named `prometheus-data`,
`grafana-data`, `loki-data`, and `alloy-data` volumes preserve metrics, dashboards, logs,
and file read positions across ordinary restarts.

## Publish Grafana at grafana.popn.gg

1. Create a DNS `A` record for `grafana.popn.gg` pointing to the Oracle server public IP.
   When using Cloudflare, use DNS-only mode until the certificate is issued, then enable
   the proxy with SSL/TLS mode **Full (strict)**.
2. Confirm ports 80 and 443 reach the server and copy the tracked Nginx virtual host:

```bash
sudo cp deploy/nginx/grafana.popn.gg.conf \
  /etc/nginx/sites-available/grafana.popn.gg
sudo ln -s /etc/nginx/sites-available/grafana.popn.gg \
  /etc/nginx/sites-enabled/grafana.popn.gg
sudo nginx -t
sudo systemctl reload nginx
```

   If the symlink already exists, do not recreate it; replace the configuration file and
   run `nginx -t` before reloading.
3. Issue and install the certificate, then verify automatic renewal:

```bash
sudo certbot --nginx -d grafana.popn.gg
sudo certbot renew --dry-run
```

4. Restart Grafana after setting `GRAFANA_DOMAIN` so its root URL and secure cookies match:

```bash
docker compose --env-file .env -f deploy/compose.yml \
  -f deploy/compose.monitoring.yml up -d --force-recreate grafana
```

Open `https://grafana.popn.gg` and sign in with `GRAFANA_ADMIN_USER` and
`GRAFANA_ADMIN_PASSWORD`. Anonymous access and self-service signup are disabled, domain
validation is enforced, and the session cookie is HTTPS-only with SameSite Strict. The
provisioned dashboard is in **Dashboards > POPN.GG > POPN.GG Production Overview**.

The provisioned production dashboard is editable in the Grafana UI. Use a panel's
**Edit** action and **Save dashboard** to persist changes in the Grafana data volume.
A later deployment that changes the provisioned dashboard JSON may replace UI changes,
so export important edits as JSON and commit them back to
`deploy/monitoring/grafana/dashboards/`.

For stronger protection, place a Cloudflare Access self-hosted application in front of
`grafana.popn.gg` and allow only administrator email addresses. Keep Grafana's own login
enabled as a second layer. Do not enable Grafana auth-proxy unless its trusted-header and
network boundaries are separately reviewed.

## Access Prometheus

Prometheus is available to administrators through a similar optional tunnel:

```bash
ssh -L 9090:127.0.0.1:9090 ubuntu@your-server
```

Open `http://127.0.0.1:9090/targets` and confirm that `popngg-api` is `UP`. Prometheus has
no built-in login, so never expose this tunnel to other network interfaces.

External uptime monitoring should use `https://api.popn.gg/health`. The previous
`/actuator/health` URL is now available only on the internal management port.

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
  -f deploy/compose.monitoring.yml logs --tail=100 prometheus loki alloy grafana
docker compose --env-file .env -f deploy/compose.yml exec prometheus \
  wget -qO- http://api:9091/actuator/prometheus | head
curl -fsS http://127.0.0.1:9090/api/v1/targets
curl -fsS http://127.0.0.1:3100/ready
curl -fsSG http://127.0.0.1:3100/loki/api/v1/query \
  --data-urlencode 'query={job="popngg-api"}'
curl -fsS -H 'Host: grafana.popn.gg' http://127.0.0.1:3000/api/health
curl -fsS https://grafana.popn.gg/api/health
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
docker volume inspect popngg_prometheus-data popngg_grafana-data \
  popngg_loki-data popngg_alloy-data api-logs
curl -fsS http://127.0.0.1:9090/api/v1/status/flags
```

Prometheus keeps at most seven days of blocks and targets 1GB of retained blocks. WAL and
head chunks are not immediately removable, so actual volume use can temporarily exceed the
retention size. Loki uses its TSDB Compactor to retain seven days of logs. Retention is
time-based, not disk-size-based, so monitor host disk capacity separately.

If the target is `DOWN`, confirm the API is healthy, both services use the same Compose
project, and the target is exactly `api:9091`. If Grafana has no
dashboard or datasource, inspect its logs for provisioning errors and verify the read-only
mounts under `/etc/grafana/provisioning` and `/var/lib/grafana/dashboards`. If only a panel
is empty, query its metric directly in Prometheus; a metric may require traffic or DB pool
activity before it exists.
