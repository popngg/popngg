package gg.popn.infra.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.common.ErrorNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscordErrorNotificationAdapter implements ErrorNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(DiscordErrorNotificationAdapter.class);
    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();
    private final String webhookUrl;
    private final String grafanaUrl;
    private final String releaseVersion;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public DiscordErrorNotificationAdapter(
            @Value("${popngg.discord.error-webhook-url:}") String webhookUrl,
            @Value("${popngg.monitoring.grafana-url:https://grafana.popn.gg}") String grafanaUrl,
            @Value("${POPNGG_RELEASE_VERSION:unknown}") String releaseVersion,
            ObjectMapper mapper) {
        this.webhookUrl = webhookUrl;
        this.grafanaUrl = stripTrailingSlash(grafanaUrl);
        this.releaseVersion = releaseVersion;
        this.mapper = mapper;
    }

    @Override
    public void notifyServerError(String method, String path, String exceptionType,
                                  String exceptionMessage, String rootCause, String traceId) {
        if (webhookUrl.isBlank()) return;
        String key = method + ":" + path + ":" + exceptionType;
        Instant now = Instant.now();
        Instant previous = lastSent.put(key, now);
        if (previous != null && previous.isAfter(now.minusSeconds(300))) return;
        lastSent.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(3600)));
        try {
            Map<String, Object> embed = Map.of(
                    "title", "API 서버 오류",
                    "description", safe(exceptionMessage, 1000),
                    "color", 15_158_332,
                    "timestamp", now.toString(),
                    "fields", List.of(
                            field("요청", safe(method, 16) + " " + safe(path, 500), false),
                            field("예외", safe(exceptionType, 250), true),
                            field("추적 ID", safe(traceId, 250), true),
                            field("근본 원인", safe(rootCause, 1000), false),
                            field("배포 버전", safe(releaseVersion, 250), false),
                            field("관측 링크", observationLinks(now, traceId), false)),
                    "footer", Map.of("text", "같은 요청·예외 알림은 5분 동안 중복 억제됩니다."));
            String body = mapper.writeValueAsString(Map.of(
                    "username", "popngg error monitor",
                    "allowed_mentions", Map.of("parse", List.of()),
                    "embeds", List.of(embed)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl)).timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(exception -> { log.warn("Error Discord notification failed.", exception); return null; });
        } catch (Exception exception) {
            log.warn("Could not prepare error Discord notification.", exception);
        }
    }

    private static Map<String, Object> field(String name, String value, boolean inline) {
        return Map.of("name", name, "value", value, "inline", inline);
    }

    private String observationLinks(Instant occurredAt, String traceId) throws Exception {
        long from = occurredAt.minus(Duration.ofMinutes(5)).toEpochMilli();
        long to = occurredAt.plus(Duration.ofMinutes(10)).toEpochMilli();
        String dashboard = grafanaUrl + "/d/popngg-production-overview/popn-gg-production-overview"
                + "?from=" + from + "&to=" + to + "&timezone=browser&var-job=popngg-api";

        String expression = "{job=\"popngg-api\"} |= \"" + logQlString(traceId) + "\"";
        Map<String, Object> query = Map.of(
                "refId", "A",
                "datasource", Map.of("uid", "loki", "type", "loki"),
                "expr", expression,
                "queryType", "range");
        Map<String, Object> pane = Map.of(
                "datasource", "loki",
                "queries", List.of(query),
                "range", Map.of("from", Long.toString(from), "to", Long.toString(to)));
        String panes = encode(mapper.writeValueAsString(Map.of("incident", pane)));
        String logs = grafanaUrl + "/explore?schemaVersion=1&orgId=1&panes=" + panes;
        return "[성능 대시보드](" + dashboard + ") · [관련 로그](" + logs + ")";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String logQlString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://grafana.popn.gg";
        String stripped = value.strip();
        while (stripped.endsWith("/")) stripped = stripped.substring(0, stripped.length() - 1);
        return stripped;
    }

    private static String safe(String value, int limit) {
        if (value == null || value.isBlank()) return "-";
        String cleaned = value.replace("`", "'").replace("@", "＠").replace("\n", " ").replace("\r", " ");
        return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit - 1) + "…";
    }
}
