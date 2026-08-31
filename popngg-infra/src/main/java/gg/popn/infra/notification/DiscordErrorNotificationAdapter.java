package gg.popn.infra.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.common.ErrorNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DiscordErrorNotificationAdapter implements ErrorNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(DiscordErrorNotificationAdapter.class);
    private final Map<String, Instant> lastSent = new ConcurrentHashMap<>();
    private final String webhookUrl;
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public DiscordErrorNotificationAdapter(
            @Value("${popngg.discord.error-webhook-url:}") String webhookUrl, ObjectMapper mapper) {
        this.webhookUrl = webhookUrl;
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
                            field("근본 원인", safe(rootCause, 1000), false)),
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

    private static String safe(String value, int limit) {
        if (value == null || value.isBlank()) return "-";
        String cleaned = value.replace("`", "'").replace("@", "＠").replace("\n", " ").replace("\r", " ");
        return cleaned.length() <= limit ? cleaned : cleaned.substring(0, limit - 1) + "…";
    }
}
