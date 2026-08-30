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
            String content = "**[API 5xx]**\n발생 시각: `%s`\n메서드: `%s`\n경로: `%s`\n예외: `%s`\n메시지: `%s`\n근본 원인: `%s`\n추적 ID: `%s`".formatted(
                    now, safe(method), safe(path), safe(exceptionType), safe(exceptionMessage),
                    safe(rootCause), safe(traceId));
            String body = mapper.writeValueAsString(Map.of("content", content));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl)).timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(exception -> { log.warn("Error Discord notification failed.", exception); return null; });
        } catch (Exception exception) {
            log.warn("Could not prepare error Discord notification.", exception);
        }
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return "-";
        String cleaned = value.replace("`", "'").replace("@", "＠").replace("\n", " ").replace("\r", " ");
        return cleaned.length() <= 200 ? cleaned : cleaned.substring(0, 199) + "…";
    }
}
