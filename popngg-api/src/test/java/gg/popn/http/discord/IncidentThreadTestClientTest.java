package gg.popn.http.discord;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentThreadTestClientTest {
    @Test
    void acceptsOnlyIncidentBotAcceptedResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/test", exchange -> {
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        try {
            IncidentThreadTestClient client = new IncidentThreadTestClient(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/", HttpClient.newHttpClient());
            assertThat(client.requestTest()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failsClosedWhenIncidentBotIsUnavailable() {
        IncidentThreadTestClient client = new IncidentThreadTestClient(
                "http://127.0.0.1:1", HttpClient.newHttpClient());
        assertThat(client.requestTest()).isFalse();
    }
}
