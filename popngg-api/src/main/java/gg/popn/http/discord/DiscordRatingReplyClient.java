package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class DiscordRatingReplyClient {
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String apiBase;

    DiscordRatingReplyClient(ObjectMapper mapper) {
        this(mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                "https://discord.com/api/v10");
    }

    DiscordRatingReplyClient(ObjectMapper mapper, HttpClient client, String apiBase) {
        this.mapper = mapper;
        this.client = client;
        this.apiBase = apiBase.replaceAll("/+$", "");
    }

    void send(JsonNode root, DiscordRatingImage.Result result) throws Exception {
        send(root, result.content(), result.filename(), result.png(), result.hasImage());
    }

    void send(JsonNode root, DiscordAchievementConstantImage.Result result) throws Exception {
        send(root, result.content(), result.filename(), result.png(), result.hasImage());
    }

    private void send(JsonNode root, String resultContent, String resultFilename, byte[] resultPng,
                      boolean hasImage) throws Exception {
        String application = root.path("application_id").asText();
        String token = root.path("token").asText();
        if (!application.matches("[0-9]+") || !token.matches("[A-Za-z0-9._-]{1,1024}"))
            throw new IllegalArgumentException("Invalid interaction reply");

        HttpRequest.BodyPublisher body;
        String contentType;
        Map<String, Object> payload = Map.of(
                "content", resultContent,
                "allowed_mentions", Map.of("parse", List.of()),
                "attachments", hasImage
                        ? List.of(Map.of("id", 0, "filename", resultFilename)) : List.of());
        if (hasImage) {
            String boundary = "popngg-" + UUID.randomUUID();
            body = HttpRequest.BodyPublishers.ofByteArray(multipart(
                    boundary, mapper.writeValueAsBytes(payload), resultFilename, resultPng));
            contentType = "multipart/form-data; boundary=" + boundary;
        } else {
            body = HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(payload));
            contentType = "application/json";
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiBase + "/webhooks/"
                        + application + "/" + token + "/messages/@original"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", contentType)
                .method("PATCH", body)
                .build();
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 == 2) return;
            if (response.statusCode() != 404 && response.statusCode() != 429) break;
            Thread.sleep(300L * (attempt + 1));
        }
        throw new IllegalStateException("Discord reply failed");
    }

    private static byte[] multipart(String boundary, byte[] payload, String filename, byte[] png)
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"payload_json\"\r\n"
                + "Content-Type: application/json\r\n\r\n");
        output.write(payload);
        write(output, "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\""
                + filename + "\"\r\nContent-Type: image/png\r\n\r\n");
        output.write(png);
        write(output, "\r\n--" + boundary + "--\r\n");
        return output.toByteArray();
    }

    private static void write(ByteArrayOutputStream output, String value) throws Exception {
        output.write(value.getBytes(StandardCharsets.UTF_8));
    }
}
