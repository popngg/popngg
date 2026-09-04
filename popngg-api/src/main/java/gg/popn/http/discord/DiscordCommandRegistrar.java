package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
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
                            "options", createOptions()),
                    Map.of("name", "곡조회", "description", "등록된 곡 검색", "type", 1,
                            "options", List.of(Map.of("type", 3, "name", "검색어",
                                    "description", "곡명, 장르 또는 아티스트", "required", true))),
                    Map.of("name", "곡수정", "description", "기존 곡과 채보 수정", "type", 1,
                            "options", updateOptions()),
                    Map.of("name", "비밀번호초기화", "description", "특정 사용자의 임시 비밀번호 발급", "type", 1,
                            "options", List.of(option(3, "팝토모_id", "예: 1234-5678-9012", true))),
                    Map.of("name", "정보보완목록", "description", "등록된 곡의 불완전한 정보 조회 및 수정", "type", 1),
                    Map.of("name", "미등록목록", "description", "최근 미등록 곡과 채보 조회", "type", 1),
                    Map.of("name", "배포버전", "description", "현재 실행 중인 API 버전과 커밋 확인", "type", 1),
                    Map.of("name", "성능대시보드", "description", "현재 운영 성능 Grafana 대시보드 열기", "type", 1),
                    Map.of("name", "장애상태확인", "description", "현재 지표 판정만 확인하며 알림은 전송하지 않음", "type", 1),
                    Map.of("name", "에러알림테스트", "description", "API 예외용 error-log Webhook 전달 테스트", "type", 1)));
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

    private static List<Map<String, Object>> createOptions() {
        return List.of(
                option(11, "자켓", "자켓 이미지", true), option(3, "추가일", "YYYY-MM-DD", true),
                option(3, "곡명", "곡명", true), option(3, "장르", "장르", true),
                option(3, "아티스트", "아티스트", true), option(4, "버전", "예: 29", true),
                upperOption(true), option(4, "l", "L 레벨 (없으면 생략)", false),
                option(4, "n", "N 레벨 (없으면 생략)", false),
                option(4, "h", "H 레벨 (없으면 생략)", false),
                option(4, "ex", "EX 레벨 (없으면 생략)", false));
    }

    private static List<Map<String, Object>> updateOptions() {
        return List.of(
                option(4, "song_id", "수정할 songId", true),
                option(11, "자켓", "교체할 자켓 이미지", false),
                option(3, "추가일", "변경할 날짜 YYYY-MM-DD", false),
                option(3, "곡명", "변경할 곡명", false), option(3, "장르", "변경할 장르", false),
                option(3, "아티스트", "변경할 아티스트", false), option(4, "버전", "변경할 버전", false),
                upperOption(false), option(4, "l", "변경할 L 레벨", false),
                option(4, "n", "변경할 N 레벨", false), option(4, "h", "변경할 H 레벨", false),
                option(4, "ex", "변경할 EX 레벨", false));
    }

    private static Map<String, Object> option(int type, String name, String description, boolean required) {
        return Map.of("type", type, "name", name, "description", description, "required", required);
    }

    private static Map<String, Object> upperOption(boolean required) {
        return Map.of("type", 3, "name", "upper", "description", "UPPER 여부 o/x", "required", required,
                "choices", List.of(Map.of("name", "o", "value", "o"), Map.of("name", "x", "value", "x")));
    }
}
