package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordCommandRegistrarTest {
    @Test
    void skipsIncompleteConfigurationAndRegistersGuildCommands() throws Exception {
        new DiscordCommandRegistrar("", "", "", new ObjectMapper(),
                HttpClient.newHttpClient(), "http://localhost").run(null);

        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/applications/app/guilds/guild/commands", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(200, 0); exchange.close();
        });
        server.start();
        try {
            new DiscordCommandRegistrar("app", "guild", "token", new ObjectMapper(),
                    HttpClient.newHttpClient(), "http://127.0.0.1:" + server.getAddress().getPort()).run(null);
            assertThat(body.get()).contains("곡추가", "곡수정", "미등록목록");
            var commands = new ObjectMapper().readTree(body.get());
            assertThat(commands.get(0).path("options").size()).isEqualTo(11);
            assertThat(commands.get(2).path("options").size()).isEqualTo(12);
        } finally { server.stop(0); }
    }
}
