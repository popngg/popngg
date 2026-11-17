package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscordRatingReplyClientTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void patchesTheDeferredReplyWithAPngAttachment() throws Exception {
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(exchange.getRequestBody().readAllBytes());
            exchange.sendResponseHeaders(204, -1);
        });
        try {
            client(server).send(root(), new DiscordRatingImage.Result(
                    "완료", "popngg-lv49-cpi-1234-5678-9012.png", new byte[]{1, 2, 3}));
            String sent = new String(body.get(), StandardCharsets.ISO_8859_1);
            assertThat(contentType.get()).startsWith("multipart/form-data; boundary=popngg-");
            assertThat(sent).contains("payload_json", "files[0]", "image/png",
                    "popngg-lv49-cpi-1234-5678-9012.png", "attachments");
            assertThat(body.get()).endsWith((byte)'\r', (byte)'\n');
        } finally { server.stop(0); }
    }

    @Test
    void sendsFailuresAsJsonAndRetriesAReplyRace() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if (attempts.getAndIncrement() == 0) exchange.sendResponseHeaders(404, -1);
            else exchange.sendResponseHeaders(204, -1);
        });
        try {
            client(server).send(root(), DiscordRatingImage.Result.text("실패 안내"));
            assertThat(attempts.get()).isEqualTo(2);
            assertThat(body.get()).contains("실패 안내", "allowed_mentions", "attachments");
        } finally { server.stop(0); }
    }

    @Test
    void rejectsInvalidInteractionCredentialsAndDiscordFailures() throws Exception {
        var invalid = mapper.createObjectNode().put("application_id", "bad").put("token", "token");
        assertThatThrownBy(() -> new DiscordRatingReplyClient(mapper).send(
                invalid, DiscordRatingImage.Result.text("x"))).isInstanceOf(IllegalArgumentException.class);

        HttpServer server = server(exchange -> exchange.sendResponseHeaders(500, -1));
        try {
            assertThatThrownBy(() -> client(server).send(root(), DiscordRatingImage.Result.text("x")))
                    .isInstanceOf(IllegalStateException.class);
        } finally { server.stop(0); }
    }

    private DiscordRatingReplyClient client(HttpServer server) {
        return new DiscordRatingReplyClient(mapper, HttpClient.newHttpClient(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private com.fasterxml.jackson.databind.node.ObjectNode root() {
        return mapper.createObjectNode().put("application_id", "123").put("token", "valid-token");
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/webhooks/123/valid-token/messages/@original", handler);
        server.start();
        return server;
    }
}
