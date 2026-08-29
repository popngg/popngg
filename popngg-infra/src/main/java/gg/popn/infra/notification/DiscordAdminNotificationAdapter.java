package gg.popn.infra.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.song.port.out.AdminNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class DiscordAdminNotificationAdapter implements AdminNotificationPort {
    private static final Logger log = LoggerFactory.getLogger(DiscordAdminNotificationAdapter.class);
    private final String webhookUrl;
    private final ObjectMapper mapper;

    public DiscordAdminNotificationAdapter(
            @Value("${popngg.discord.admin-webhook-url:}") String webhookUrl, ObjectMapper mapper) {
        this.webhookUrl = webhookUrl;
        this.mapper = mapper;
    }

    @Override
    public void send(String message) {
        if (webhookUrl.isBlank()) return;
        try {
            String body = mapper.writeValueAsString(Map.of("content", message));
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl)).timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(exception -> { log.warn("Admin Discord notification failed.", exception); return null; });
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("Could not prepare admin Discord notification.", exception);
        }
    }
}
