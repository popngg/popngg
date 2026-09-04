import json
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PROMETHEUS = os.getenv("PROMETHEUS_URL", "http://prometheus:9090").rstrip("/")
LOKI = os.getenv("LOKI_URL", "http://loki:3100").rstrip("/")
WEBHOOK = os.getenv("DISCORD_ERROR_WEBHOOK_URL", "").strip()
BOT_TOKEN = os.getenv("DISCORD_BOT_TOKEN", "").strip()
GRAFANA = os.getenv("GRAFANA_PUBLIC_URL", "https://grafana.popn.gg").rstrip("/")
INTERVAL = int(os.getenv("INCIDENT_POLL_INTERVAL_SECONDS", "30"))
USER_AGENT = "popngg-incident-monitor/1.0 (+https://popn.gg)"

RULES = {
    "api-down": ("API 응답 불가", 'up{job="popngg-api"}', lambda value: value < 1, 120),
    "high-5xx": ("API 5xx 증가", 'sum(rate(http_server_requests_seconds_count{job="popngg-api",uri!~"/actuator.*",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{job="popngg-api",uri!~"/actuator.*"}[5m]))', lambda value: value >= .05, 180),
    "high-p95": ("API P95 지연", 'histogram_quantile(0.95,sum by(le)(rate(http_server_requests_seconds_bucket{job="popngg-api",uri!~"/actuator.*"}[5m])))', lambda value: value >= 5, 300),
    "db-pending": ("DB 연결 대기", 'max(hikaricp_connections_pending{job="popngg-api"})', lambda value: value >= 1, 120),
    "high-cpu": ("시스템 CPU 과부하", 'system_cpu_usage{job="popngg-api"}', lambda value: value >= .85, 600),
}

states = {key: {"since": None, "thread": None, "started": None} for key in RULES}


def request_json(url, payload=None, headers=None, timeout=4):
    data = None if payload is None else json.dumps(payload).encode()
    request = urllib.request.Request(url, data=data, headers={
        "Content-Type": "application/json",
        "User-Agent": USER_AGENT,
        **(headers or {}),
    })
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read()
            return json.loads(body) if body else {}
    except urllib.error.HTTPError as error:
        detail = error.read().decode(errors="replace")[:500]
        raise RuntimeError(f"HTTP {error.code} from {request.full_url}: {detail}") from error


def query(expression):
    url = PROMETHEUS + "/api/v1/query?" + urllib.parse.urlencode({"query": expression})
    result = request_json(url).get("data", {}).get("result", [])
    if not result:
        return None
    value = float(result[0]["value"][1])
    return value if value == value and abs(value) != float("inf") else None


def dashboard_url(now):
    start = int((now - 300) * 1000)
    end = int((now + 600) * 1000)
    return f"{GRAFANA}/d/popngg-production-overview/popn-gg-production-overview?from={start}&to={end}&timezone=browser&var-job=popngg-api"


def recent_error_summary(now=None):
    end = now or time.time()
    params = urllib.parse.urlencode({
        "query": '{job="popngg-api"} | json | level="ERROR"',
        "start": str(int((end - 300) * 1_000_000_000)),
        "end": str(int(end * 1_000_000_000)),
        "limit": "20",
        "direction": "backward",
    })
    try:
        streams = request_json(LOKI + "/loki/api/v1/query_range?" + params).get("data", {}).get("result", [])
        entries = []
        for stream in streams:
            for _, line in stream.get("values", []):
                try:
                    event = json.loads(line)
                except json.JSONDecodeError:
                    continue
                message = str(event.get("message", "error"))[:120].replace("`", "'")
                route = str(event.get("uri", "unknown"))[:80]
                trace = str(event.get("traceId", "-"))[:40]
                item = f"- `{route}` · {message} · trace `{trace}`"
                if item not in entries:
                    entries.append(item)
                if len(entries) == 5:
                    break
            if len(entries) == 5:
                break
        return "\n".join(entries) if entries else "- 최근 5분간 구조화된 ERROR 로그 없음"
    except Exception as exception:
        print(f"Loki error summary failed: {exception}", flush=True)
        return "- Loki 오류 요약 조회 실패 (Grafana에서 직접 확인 필요)"


def webhook_url(thread_id=None):
    separator = "&" if "?" in WEBHOOK else "?"
    suffix = "wait=true"
    if thread_id:
        suffix += "&thread_id=" + urllib.parse.quote(thread_id)
    return WEBHOOK + separator + suffix


def open_thread(key, title, detail, test=False):
    if not WEBHOOK or not BOT_TOKEN:
        raise RuntimeError("Discord webhook or bot token is not configured")
    now = time.time()
    prefix = "🧪 장애 알림 스레드 테스트" if test else "🔴 장애 감지"
    summary = "- 테스트 요청에서는 실제 로그를 첨부하지 않음" if test else recent_error_summary(now)
    message = request_json(webhook_url(), {
        "username": "popngg incident monitor",
        "allowed_mentions": {"parse": []},
        "content": f"**{prefix}: {title}**\n{detail}\n\n**최근 5분 ERROR 요약**\n{summary}\n[Grafana에서 확인]({dashboard_url(now)})",
    })
    channel_id, message_id = str(message["channel_id"]), str(message["id"])
    thread = request_json(
        f"https://discord.com/api/v10/channels/{channel_id}/messages/{message_id}/threads",
        {"name": ("test-" if test else "incident-") + key + "-" + datetime.now(timezone.utc).strftime("%Y%m%d-%H%M"),
         "auto_archive_duration": 1440},
        {"Authorization": "Bot " + BOT_TOKEN})
    thread_id = str(thread["id"])
    request_json(webhook_url(thread_id), {
        "username": "popngg incident monitor",
        "allowed_mentions": {"parse": []},
        "content": "스레드 생성이 완료되었습니다. 상태 변화와 복구 결과가 이곳에 기록됩니다."
    })
    return thread_id


def resolve_thread(thread_id, title, started):
    duration = max(0, int(time.time() - started))
    request_json(webhook_url(thread_id), {
        "username": "popngg incident monitor",
        "allowed_mentions": {"parse": []},
        "content": f"🟢 **복구 확인: {title}**\n지속 시간: 약 {duration // 60}분 {duration % 60}초"
    })


def evaluate():
    now = time.time()
    for key, (title, expression, failing, duration) in RULES.items():
        state = states[key]
        try:
            value = query(expression)
            is_failing = value is not None and failing(value)
            if is_failing:
                state["since"] = state["since"] or now
                if state["thread"] is None and now - state["since"] >= duration:
                    state["thread"] = open_thread(key, title, f"현재 값: `{value:.3f}` · 기준 지속: `{duration // 60}분`")
                    state["started"] = now
            else:
                if state["thread"]:
                    resolve_thread(state["thread"], title, state["started"] or now)
                state.update(since=None, thread=None, started=None)
        except Exception as exception:
            print(f"incident rule {key} failed: {exception}", flush=True)


def monitor():
    while True:
        evaluate()
        time.sleep(INTERVAL)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self.send_response(200 if WEBHOOK and BOT_TOKEN else 503)
            self.end_headers()
            self.wfile.write(b"ok" if WEBHOOK and BOT_TOKEN else b"discord configuration missing")
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path != "/test":
            self.send_error(404)
            return
        self.send_response(202)
        self.end_headers()
        self.wfile.write(b"accepted")
        threading.Thread(target=self._test, daemon=True).start()

    def _test(self):
        try:
            open_thread("manual", "관리자 수동 테스트", "실제 장애 없이 스레드 생성 경로를 검증합니다.", True)
        except Exception as exception:
            print(f"incident thread test failed: {exception}", flush=True)

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    threading.Thread(target=monitor, daemon=True).start()
    ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()
