package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.tier.AchievementBoard;
import gg.popn.application.tier.AchievementBoardImagePort;
import gg.popn.application.tier.AchievementBoardService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/** Defer immediately; aggregation and jacket downloads must not delay the interaction acknowledgement. */
@Component
public class DiscordAchievementCommand {
    private static final Logger log = LoggerFactory.getLogger(DiscordAchievementCommand.class);
    private final AchievementBoardService service;
    private final AchievementBoardImagePort renderer;
    private final ObjectMapper mapper;
    private final HttpClient client;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2), task -> { var t = new Thread(task, "achievement-board"); t.setDaemon(true); return t; });
    @org.springframework.beans.factory.annotation.Autowired
    public DiscordAchievementCommand(AchievementBoardService service, AchievementBoardImagePort renderer, ObjectMapper mapper) {
        this(service, renderer, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }
    DiscordAchievementCommand(AchievementBoardService service, AchievementBoardImagePort renderer, ObjectMapper mapper, HttpClient client) {
        this.service = service; this.renderer = renderer; this.mapper = mapper; this.client = client;
    }
    public Map<String, Object> execute(JsonNode root) {
        int level = 0, page = 1;
        AchievementBoard.Metric metric = null;
        try {
            for (var option : root.path("data").path("options")) {
                switch (option.path("name").asText()) {
                    case "레벨" -> level = option.path("value").asInt();
                    case "기준" -> metric = AchievementBoard.Metric.valueOf(option.path("value").asText());
                    case "페이지" -> page = option.path("value").asInt();
                }
            }
        } catch (IllegalArgumentException e) { return error("기준은 S 또는 퍼펙트입니다."); }
        if (level < 40 || level > 50 || metric == null || page < 1) return error("레벨 40~50과 기준을 선택하고, 페이지는 1 이상으로 입력해 주세요.");
        String appId = root.path("application_id").asText(), token = root.path("token").asText();
        if (!appId.matches("[0-9]+") || !token.matches("[A-Za-z0-9._-]+")) return error("이미지 응답 정보를 확인할 수 없습니다.");
        URI response = URI.create("https://discord.com/api/v10/webhooks/" + appId + "/" + token + "/messages/@original");
        final int requestedLevel = level, requestedPage = page;
        final var requestedMetric = metric;
        try {
            worker.execute(() -> generate(response, requestedLevel, requestedMetric, requestedPage));
        } catch (RejectedExecutionException e) { return error("이미지 생성 요청이 많습니다. 잠시 후 다시 시도해 주세요."); }
        return Map.of("type", 5);
    }
    private void generate(URI response, int level, AchievementBoard.Metric metric, int page) {
        try {
            var board = service.load(level, metric);
            if (page > board.pages()) { editText(response, "페이지는 1~" + board.pages() + " 범위입니다."); return; }
            byte[] png = renderer.render(board, page);
            if (png.length > 9_500_000) throw new IllegalStateException("Image attachment too large.");
            String filename = "popngg-lv" + level + "-" + metric.name().toLowerCase(java.util.Locale.ROOT) + "-" + page + ".png";
            String boundary = "achievement-" + UUID.randomUUID();
            String payload = mapper.writeValueAsString(Map.of("content", "Lv" + level + " · " + metric
                    + " 달성자 많은 순 · " + page + "/" + board.pages() + "페이지 (전체 " + board.charts().size()
                    + "채보)\n다음 페이지: `/달성서열표 레벨:" + level + " 기준:" + (metric == AchievementBoard.Metric.S ? "S" : "퍼펙트")
                    + " 페이지:" + Math.min(page + 1, board.pages()) + "`", "allowed_mentions", Map.of("parse", List.of()),
                    "attachments", List.of(Map.of("id", 0, "filename", filename))));
            String prefix = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"payload_json\"\r\nContent-Type: application/json\r\n\r\n"
                    + payload + "\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\""
                    + filename + "\"\r\nContent-Type: image/png\r\n\r\n";
            send(HttpRequest.newBuilder(response).header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .method("PATCH", HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofString(prefix),
                            HttpRequest.BodyPublishers.ofByteArray(png), HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"))));
        } catch (Exception e) {
            // Never log interaction tokens or HTTP request URIs.
            log.warn("Achievement board generation or delivery failed: {}", e.getClass().getSimpleName());
            try { editText(response, "달성 서열표 이미지를 만들거나 전송하지 못했습니다. 잠시 후 다시 시도해 주세요."); }
            catch (Exception ignored) { log.warn("Achievement board error response delivery failed."); }
        }
    }
    private void editText(URI uri, String text) throws Exception {
        send(HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(
                        Map.of("content", text, "allowed_mentions", Map.of("parse", List.of()))), StandardCharsets.UTF_8)));
    }
    private void send(HttpRequest.Builder request) throws Exception {
        int status = client.send(request.timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status < 200 || status >= 300) throw new IllegalStateException("Discord attachment delivery failed.");
    }
    private static Map<String, Object> error(String text) { return Map.of("type", 4, "data", Map.of("content", text, "flags", 64)); }
    @PreDestroy public void close() { worker.shutdownNow(); }
}
