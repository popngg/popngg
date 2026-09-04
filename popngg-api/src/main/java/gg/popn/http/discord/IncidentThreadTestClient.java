package gg.popn.http.discord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class IncidentThreadTestClient {
    private final String incidentBotUrl;
    private final HttpClient client;

    public IncidentThreadTestClient(
            @Value("${popngg.monitoring.incident-bot-url:http://incident-bot:8080}") String incidentBotUrl) {
        this(incidentBotUrl, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build());
    }

    IncidentThreadTestClient(String incidentBotUrl, HttpClient client) {
        this.incidentBotUrl = incidentBotUrl.replaceAll("/+$", "");
        this.client = client;
    }

    public boolean requestTest() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(incidentBotUrl + "/test"))
                    .timeout(Duration.ofSeconds(1)).POST(HttpRequest.BodyPublishers.noBody()).build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status == 202;
        } catch (Exception exception) {
            return false;
        }
    }
}
