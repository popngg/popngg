package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DiscordCommandRegistrar implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DiscordCommandRegistrar.class);
    private final String applicationId;
    private final String guildId;
    private final String botToken;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final String apiBase;

    public DiscordCommandRegistrar(
            @Value("${popngg.discord.application-id:}") String applicationId,
            @Value("${popngg.discord.guild-id:}") String guildId,
            @Value("${popngg.discord.bot-token:}") String botToken,
            ObjectMapper mapper) {
        this.applicationId = applicationId;
        this.guildId = guildId;
        this.botToken = botToken;
        this.mapper = mapper;
        this.client = HttpClient.newHttpClient();
        this.apiBase = "https://discord.com/api/v10";
    }

    DiscordCommandRegistrar(String applicationId, String guildId, String botToken,
                            ObjectMapper mapper, HttpClient client, String apiBase) {
        this.applicationId=applicationId; this.guildId=guildId; this.botToken=botToken;
        this.mapper=mapper; this.client=client; this.apiBase=apiBase.replaceAll("/+$", "");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (applicationId.isBlank() || guildId.isBlank() || botToken.isBlank()) {
            log.info("Discord command registration is disabled because its configuration is incomplete.");
            return;
        }
        try {
            String body = mapper.writeValueAsString(List.of(
                    Map.of("name", "곡추가", "description", "관리자 전용 곡과 채보 등록", "type", 1,
                            "options", List.of(
                                    Map.of("type", 11, "name", "자켓", "description", "자켓 이미지", "required", true),
                                    Map.of("type", 3, "name", "추가일", "description", "YYYY-MM-DD", "required", true))),
                    Map.of("name", "곡조회", "description", "등록된 곡 검색", "type", 1,
                            "options", List.of(Map.of("type", 3, "name", "검색어",
                                    "description", "곡명, 장르 또는 아티스트", "required", true))),
                    Map.of("name", "곡수정", "description", "기존 곡과 채보 수정", "type", 1,
                            "options", List.of(
                                    Map.of("type", 4, "name", "song_id", "description", "수정할 songId", "required", true),
                                    Map.of("type", 11, "name", "자켓", "description", "교체할 자켓 이미지", "required", false),
                                    Map.of("type", 3, "name", "추가일", "description", "변경할 날짜 YYYY-MM-DD", "required", false))),
                    Map.of("name", "미등록목록", "description", "최근 미등록 곡과 채보 조회", "type", 1)));
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                        (apiBase + "/applications/%s/guilds/%s/commands")
                                .formatted(applicationId, guildId)))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bot " + botToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Discord command registration failed with status {}.", response.statusCode());
                return;
            }
            log.info("Discord guild commands registered successfully.");
        } catch (Exception exception) {
            log.warn("Could not register Discord guild commands.", exception);
        }
    }
}
