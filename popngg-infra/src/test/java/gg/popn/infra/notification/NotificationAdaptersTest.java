package gg.popn.infra.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.playdata.dto.command.ImportPlaydataCommand;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationAdaptersTest {
    @Test
    void disabledDiscordAdaptersAreNoOps() {
        ObjectMapper mapper = new ObjectMapper();
        new DiscordAdminNotificationAdapter("", mapper).send("message");
        var errors = new DiscordErrorNotificationAdapter("", "https://grafana.example/", "test-release", mapper);
        errors.notifyServerError("GET", "/path", "Failure", "message", "cause", "trace");
        var unknown = new DiscordUnknownChartNotifier("", mapper, java.net.http.HttpClient.newHttpClient());
        unknown.notifyUnknownCharts(1, "user", List.of(new ImportPlaydataCommand.Row(
                null, null, 4, false, null, "song", "genre", 1, 1, 1)));
    }

    @Test
    void sendsDiscordNotificationsAndSuppressesDuplicateErrors() throws Exception {
        CountDownLatch requests = new CountDownLatch(3);
        AtomicReference<String> errorPayload = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/webhook", exchange -> {
            String payload = new String(exchange.getRequestBody().readAllBytes());
            if (payload.contains("API 서버 오류")) errorPayload.set(payload);
            exchange.sendResponseHeaders(204, -1); exchange.close(); requests.countDown();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/webhook";
            ObjectMapper mapper = new ObjectMapper();
            new DiscordAdminNotificationAdapter(url, mapper).send("admin");
            var errors = new DiscordErrorNotificationAdapter(
                    url, "https://grafana.example/", "2026.09.04-test", mapper);
            errors.notifyServerError("GET", "/x", "Boom", "message @everyone", "cause", "trace");
            errors.notifyServerError("GET", "/x", "Boom", "message @everyone", "cause", "trace");
            new DiscordUnknownChartNotifier(url, mapper, java.net.http.HttpClient.newHttpClient())
                    .notifyUnknownCharts(1, "user", List.of(new ImportPlaydataCommand.Row(
                            null, null, 4, false, null, "song", "genre", 1, 1, 1)));
            assertThat(requests.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(errorPayload.get())
                    .contains("embeds", "allowed_mentions", "추적 ID", "trace")
                    .contains("배포 버전", "2026.09.04-test")
                    .contains("https://grafana.example/d/popngg-production-overview")
                    .contains("https://grafana.example/explore?schemaVersion=1")
                    .doesNotContain("@everyone");
        } finally { server.stop(0); }
    }

    @Test
    void s3AdapterUsesConfiguredKeysAndPublicUrl() {
        S3Client s3 = mock(S3Client.class);
        var storage = new S3JacketStorageAdapter(s3, "bucket", "static", "https://static.popn.gg/");
        assertThat(storage.uploadPng("hash", new byte[]{1})).isEqualTo("https://static.popn.gg/hash.png");
        doThrow(NoSuchKeyException.builder().message("missing").build())
                .when(s3).headObject(any(java.util.function.Consumer.class));
        assertThat(storage.copy("old", "new")).isEqualTo("https://static.popn.gg/new.png");
        String backup = storage.replacePng("hash", new byte[]{2});
        assertThat(backup).startsWith("backup/hash/");
        storage.restore("hash", backup);
        storage.delete("hash");
        verify(s3, atLeastOnce()).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
}
