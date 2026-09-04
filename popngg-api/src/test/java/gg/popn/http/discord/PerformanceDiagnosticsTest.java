package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceDiagnosticsTest {
    @Test
    void collectsPrometheusInstantQueries() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/query", exchange -> {
            requests.incrementAndGet();
            byte[] body = ("{\"status\":\"success\",\"data\":{\"resultType\":\"vector\","
                    + "\"result\":[{\"metric\":{},\"value\":[1,\"0.25\"]}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            PerformanceDiagnostics diagnostics = new PerformanceDiagnostics(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/",
                    new ObjectMapper(), HttpClient.newHttpClient());
            PerformanceDiagnostics.Snapshot snapshot = diagnostics.snapshot();
            assertThat(snapshot.available()).isTrue();
            assertThat(snapshot.value("requestRate")).isEqualTo(0.25);
            assertThat(snapshot.value("blockedThreads")).isEqualTo(0.25);
            assertThat(requests.get()).isEqualTo(9);
        } finally {
            server.stop(0);
        }
    }
}
