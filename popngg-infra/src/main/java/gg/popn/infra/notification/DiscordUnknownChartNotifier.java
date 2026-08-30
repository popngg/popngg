package gg.popn.infra.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import gg.popn.application.playdata.port.out.UnknownChartNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class DiscordUnknownChartNotifier implements UnknownChartNotifier {
    private static final Logger log = LoggerFactory.getLogger(DiscordUnknownChartNotifier.class);
    private static final int MAX_ROWS = 8;
    private static final int MAX_VALUE_LENGTH = 50;

    private final String webhookUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public DiscordUnknownChartNotifier(
            @Value("${popngg.discord.admin-webhook-url:}") String webhookUrl,
            ObjectMapper objectMapper) {
        this(webhookUrl, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build());
    }

    DiscordUnknownChartNotifier(String webhookUrl, ObjectMapper objectMapper,
                                HttpClient httpClient) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.strip();
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public void notifyUnknownCharts(long renewLogId, String poptomoId,
                                    List<ImportPlaydataCommand.Row> rows) {
        if (webhookUrl.isBlank() || rows.isEmpty()) {
            return;
        }
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("content",
                    message(renewLogId, poptomoId, rows)));
        } catch (JsonProcessingException exception) {
            log.warn("Could not serialize unknown chart notification for renew log {}.",
                    renewLogId, exception);
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.warn("Discord rejected unknown chart notification for renew log {} with status {}.",
                                renewLogId, response.statusCode());
                    }
                })
                .exceptionally(exception -> {
                    log.warn("Could not send unknown chart notification for renew log {}.",
                            renewLogId, exception);
                    return null;
                });
    }

    private static String message(long renewLogId, String poptomoId,
                                  List<ImportPlaydataCommand.Row> rows) {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        for (ImportPlaydataCommand.Row row : rows) {
            items.add("- %s / %s / %s".formatted(
                    safe(row.songName()), safe(row.genreName()), safe(row.artistName())));
        }
        List<String> visible = items.stream().limit(MAX_ROWS).toList();
        StringBuilder message = new StringBuilder()
                .append("**[미등록 곡/채보 감지]**\n")
                .append("갱신 로그: `").append(renewLogId).append("`\n")
                .append("사용자: `").append(safe(poptomoId)).append("`\n")
                .append(String.join("\n", visible));
        if (items.size() > MAX_ROWS) {
            message.append("\n- 외 ").append(items.size() - MAX_ROWS).append("건");
        }
        return message.toString();
    }

    private static String safe(Object value) {
        if (value == null || value.toString().isBlank()) return "-";
        String sanitized = value.toString().replace("`", "'")
                .replace("@", "＠")
                .replace("\r", " ").replace("\n", " ");
        return sanitized.length() <= MAX_VALUE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_VALUE_LENGTH - 1) + "…";
    }
}
