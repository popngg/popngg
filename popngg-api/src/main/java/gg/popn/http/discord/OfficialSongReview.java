package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.port.out.OfficialSongSource;
import gg.popn.application.song.port.out.ReviewedSongRegistrationPort;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/** Called only after the interaction controller verifies the signature and administrator role. */
@Component
public class OfficialSongReview {
    private final OfficialSongSource source;
    private final ReviewedSongRegistrationPort registration;
    private final Executor executor;
    private final Reply reply;
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Map<String, Operation> operations = new ConcurrentHashMap<>();
    private static final List<String> LABELS = List.of("L", "N", "H", "EX");

    @Autowired
    public OfficialSongReview(OfficialSongSource source, ReviewedSongRegistrationPort registration, ObjectMapper mapper) {
        this(source, registration, new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), runnable -> {
                    Thread thread = new Thread(runnable, "official-song-review");
                    thread.setDaemon(true);
                    return thread;
                }), (root, data) -> {
                    String application = root.path("application_id").asText();
                    String token = root.path("token").asText();
                    if (!application.matches("[0-9]+") || !token.matches("[A-Za-z0-9._-]{1,1024}"))
                        throw new IllegalArgumentException("Invalid interaction reply");
                    var request = HttpRequest.newBuilder(URI.create("https://discord.com/api/v10/webhooks/"
                                    + application + "/" + token + "/messages/@original"))
                            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                            .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data))).build();
                    var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                    for (int attempt = 0; attempt < 4; attempt++) {
                        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
                        if (response.statusCode() / 100 == 2) return;
                        // A cached lookup can finish before Discord receives our deferred acknowledgement.
                        if (response.statusCode() != 404 && response.statusCode() != 429) break;
                        Thread.sleep(300L * (attempt + 1));
                    }
                    throw new IllegalStateException("Discord reply failed");
                });
    }

    OfficialSongReview(OfficialSongSource source, ReviewedSongRegistrationPort registration, Executor executor, Reply reply) {
        this.source = source; this.registration = registration; this.executor = executor; this.reply = reply;
    }

    public Map<String, Object> start(JsonNode root, UnknownChartReportPort.Report report) {
        return defer(root, () -> {
            var candidates = source.find(report.songName(), report.artistName(), report.upper());
            if (candidates.size() != 1)
                return data("공식 정보가 없거나 후보가 여러 개입니다. 자동으로 선택하지 않았습니다. `/곡추가`로 확인해 주세요.");
            var song = candidates.getFirst();
            var existing = registration.findExisting(song.title(), song.artist(), song.upper());
            if (!existing.isEmpty()) return data("이미 등록된 곡입니다: " + existing
                    + ". 장르 변경 또는 채보 누락은 `/곡수정`으로 처리해 주세요.\n공식 장르: " + song.genre());
            cleanup();
            if (drafts.size() >= 128) return data("등록 대기 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
            String id = UUID.randomUUID().toString();
            drafts.put(id, new Draft(song, report.reportId(), owner(root), Instant.now()));
            return preview(id, song);
        });
    }

    public Map<String, Object> interact(JsonNode root) {
        cleanup();
        String custom = root.path("data").path("custom_id").asText();
        String[] parts = custom.split(":", 2);
        if (parts.length == 2 && parts[0].equals("official_status") && root.path("type").asInt() == 3) {
            Operation operation = operations.get(parts[1]);
            if (operation == null || !operation.owner.equals(owner(root)))
                return message("처리 상태가 만료되었거나 본인의 요청이 아닙니다. `/미등록목록`에서 다시 확인해 주세요.");
            var result = operation.result;
            if (result == null) {
                String content = operation.createdAt.isBefore(Instant.now().minusSeconds(150))
                        ? "처리가 예상보다 오래 걸리고 있습니다. 등록을 다시 누르지 말고 잠시 후 상태를 확인해 주세요."
                        : "공식 정보 조회 또는 등록을 처리 중입니다. 아직 결과가 없습니다. 잠시 후 다시 확인해 주세요.";
                return Map.of("type", 7, "data", pending(parts[1], content));
            }
            return Map.of("type", 7, "data", result);
        }
        Draft draft = parts.length == 2 ? drafts.get(parts[1]) : null;
        if (draft == null || !draft.owner().equals(owner(root))) return message("등록안이 만료되었거나 본인의 요청이 아닙니다. `/미등록목록`에서 다시 선택해 주세요.");
        if (root.path("type").asInt() == 3 && parts[0].equals("official_cancel")) {
            drafts.remove(parts[1], draft);
            return message("곡 등록을 취소했습니다.");
        }
        if (root.path("type").asInt() == 3 && parts[0].equals("official_review")) return modal(parts[1], draft.song());
        boolean direct = root.path("type").asInt() == 3 && parts[0].equals("official_confirm");
        if (!direct && (root.path("type").asInt() != 5 || !parts[0].equals("official_submit"))) return message("지원하지 않는 등록 요청입니다.");
        final CreateSongCommand command;
        try { command = direct ? defaultCommand(draft.song()) : command(draft.song(), root); }
        catch (RuntimeException exception) { return message("입력 오류: " + exception.getMessage() + " 등록안의 정보 수정 버튼을 다시 눌러 주세요."); }
        if (!drafts.remove(parts[1], draft)) return message("이미 처리된 등록 요청입니다.");
        var response = defer(root, () -> {
            var result = registration.register(draft.reportId(), command);
            return data("곡 등록 완료: **" + draft.song().title().replace("`", "'") + "** (`songId="
                    + result.songId() + "`, 채보 " + result.chartIds().size() + "개, 자켓 없음)\n점수 반영을 위해 다시 갱신해 주세요.");
        });
        if (((Map<?, ?>) response.get("data")).get("components") instanceof List<?> components && components.isEmpty())
            drafts.putIfAbsent(parts[1], draft);
        return response;
    }

    private Map<String, Object> defer(JsonNode root, Work work) {
        cleanup();
        if (operations.size() >= 128) return message("처리 대기 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        String operationId = UUID.randomUUID().toString();
        Operation operation = new Operation(owner(root), Instant.now());
        operations.put(operationId, operation);
        try {
            executor.execute(() -> {
                Map<String, Object> result;
                try { result = work.run(); }
                catch (Exception exception) {
                    result = data("공식 정보 조회 또는 등록을 완료하지 못했습니다. `/미등록목록`에서 다시 확인해 주세요. "
                            + "이미 등록된 곡이거나 공식 사이트가 일시적으로 응답하지 않을 수 있습니다.");
                    LoggerFactory.getLogger(OfficialSongReview.class).warn("Official song review failed: {}", exception.getClass().getSimpleName());
                }
                // Keep the result before sending: a failed webhook must not hide success or cause a second write.
                operation.result = result;
                try { reply.send(root, result); }
                catch (Exception exception) {
                    // Never log interaction tokens or URLs. A committed registration is not rolled back for a reply failure.
                    LoggerFactory.getLogger(OfficialSongReview.class).warn("Could not deliver official song review reply; result available via status button ({})", exception.getClass().getSimpleName());
                }
            });
            var pending = new HashMap<>(pending(operationId,
                    "공식 정보 조회 또는 등록을 처리 중입니다. 첫 조회에는 시간이 걸릴 수 있습니다. 결과가 자동으로 표시되지 않으면 아래에서 확인해 주세요. 곡 선택만으로는 등록되지 않습니다."));
            pending.put("flags", 64);
            return Map.of("type", 4, "data", pending);
        } catch (RejectedExecutionException exception) {
            operations.remove(operationId, operation);
            return message("조회 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private static Map<String, Object> pending(String id, String content) {
        return Map.of("content", content, "allowed_mentions", Map.of("parse", List.of()),
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 2, "label", "처리 상태 확인", "custom_id", "official_status:" + id)))));
    }

    private Map<String, Object> preview(String id, OfficialSongSource.Song song) {
        List<Map<String, Object>> fields = List.of(field("곡명", song.title()), field("장르", song.genre()),
                field("아티스트", song.artist()), field("공식 버전 분류 / UPPER", song.version() + " / " + song.upper()),
                field("채보 레벨", song.charts().stream().map(c -> LABELS.get(c.difficulty() - 1) + ":" + c.level()).collect(Collectors.joining(", "))),
                field("자켓", "등록하지 않음"),
                field("바로 등록 기본값 (공식 제공 정보 아님)", "특수 게이지·판정: 없음\n채보 추가 버전: 공식 버전 분류와 동일\n추가일: 등록 시각"));
        return Map.of("content", "공식 정보로 만든 등록안입니다. 아직 등록되지 않았습니다.",
                "allowed_mentions", Map.of("parse", List.of()),
                "embeds", List.of(Map.of("title", "공식 곡 정보 확인", "url", song.sourceUrl(), "fields", fields,
                        "description", "아래 기본값이 맞으면 확인·등록을 누르세요. 추가 입력 없이 등록됩니다. 특수 게이지·판정, 채보 추가 버전, 추가일을 바꾸려면 정보 수정을 선택하세요. (15분 유효)")),
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 3, "label", "확인·등록", "custom_id", "official_confirm:" + id),
                        Map.of("type", 2, "style", 2, "label", "정보 수정", "custom_id", "official_review:" + id),
                        Map.of("type", 2, "style", 4, "label", "취소", "custom_id", "official_cancel:" + id)))));
    }

    private static CreateSongCommand defaultCommand(OfficialSongSource.Song song) {
        return new CreateSongCommand(null, song.genre(), song.title(), song.artist(), song.version(), null, null,
                song.charts().stream().map(c -> new CreateSongCommand.CreateChartCommand(
                        c.difficulty(), c.level(), song.version(), song.upper(), false, false)).toList());
    }

    private Map<String, Object> modal(String id, OfficialSongSource.Song song) {
        String versions = song.charts().stream().map(c -> LABELS.get(c.difficulty() - 1) + ":" + song.version()).collect(Collectors.joining(","));
        return Map.of("type", 9, "data", Map.of("custom_id", "official_submit:" + id, "title", "자켓 없이 곡 등록 확인",
                "components", List.of(input("versions", "채보 추가 버전 확인 (공식 분류로 초안)", versions, true),
                        input("gauge", "특수 게이지 채보 (L,N,H,EX 또는 없음)", "없음", true),
                        input("judgement", "특수 판정 채보 (L,N,H,EX 또는 없음)", "없음", true),
                        input("date", "추가일 YYYY-MM-DD (모르면 공란)", "", false))));
    }

    static CreateSongCommand command(OfficialSongSource.Song song, JsonNode root) {
        Map<Integer, Integer> versions = new HashMap<>();
        for (String item : value(root, "versions").split(",", -1)) {
            String[] pair = item.strip().split(":", -1);
            if (pair.length != 2) throw new IllegalArgumentException("채보 버전은 L:29,N:29 형식입니다.");
            int difficulty = difficulty(pair[0]), version = Integer.parseInt(pair[1].strip());
            if (version < 0 || version > 99 || versions.put(difficulty, version) != null)
                throw new IllegalArgumentException("채보 버전을 확인해 주세요.");
        }
        Set<Integer> actual = song.charts().stream().map(OfficialSongSource.Chart::difficulty).collect(Collectors.toSet());
        Set<Integer> gauge = flags(value(root, "gauge")), judgement = flags(value(root, "judgement"));
        if (!versions.keySet().equals(actual) || !actual.containsAll(gauge) || !actual.containsAll(judgement))
            throw new IllegalArgumentException("공식 목록에 있는 채보만 입력해 주세요.");
        String date = value(root, "date");
        Instant addedAt = date.isBlank() ? null : LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new CreateSongCommand(null, song.genre(), song.title(), song.artist(), song.version(), null, addedAt,
                song.charts().stream().map(c -> new CreateSongCommand.CreateChartCommand(c.difficulty(), c.level(),
                        versions.get(c.difficulty()), song.upper(), gauge.contains(c.difficulty()), judgement.contains(c.difficulty()))).toList());
    }

    private static Set<Integer> flags(String value) {
        if (value.equals("없음")) return Set.of();
        return Arrays.stream(value.split(",", -1)).map(OfficialSongReview::difficulty).collect(Collectors.toSet());
    }
    private static int difficulty(String label) {
        int code = LABELS.indexOf(label.strip().toUpperCase(Locale.ROOT)) + 1;
        if (code == 0) throw new IllegalArgumentException("채보는 L,N,H,EX로 입력해 주세요.");
        return code;
    }
    private static String value(JsonNode root, String id) {
        for (var row : root.path("data").path("components")) {
            // Accept current label components and older action-row modal components.
            if (row.path("component").path("custom_id").asText().equals(id)) return row.path("component").path("value").asText().strip();
            for (var item : row.path("components"))
                if (item.path("custom_id").asText().equals(id)) return item.path("value").asText().strip();
        }
        throw new IllegalArgumentException("입력 항목이 없습니다: " + id);
    }
    private static Map<String, Object> input(String id, String label, String value, boolean required) {
        return Map.of("type", 1, "components", List.of(Map.of("type", 4, "custom_id", id, "label", label,
                "style", 1, "required", required, "value", value, "max_length", 100)));
    }
    private static Map<String, Object> field(String name, String value) { return Map.of("name", name, "value", value); }
    private static String owner(JsonNode root) { return root.path("guild_id").asText() + ":" + root.path("member").path("user").path("id").asText(); }
    private void cleanup() {
        Instant expiry = Instant.now().minusSeconds(900);
        drafts.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(expiry));
        operations.entrySet().removeIf(e -> e.getValue().createdAt.isBefore(expiry));
    }
    private static Map<String, Object> data(String content) { return Map.of("content", content, "components", List.of(), "embeds", List.of(), "allowed_mentions", Map.of("parse", List.of())); }
    private static Map<String, Object> message(String content) { var data = new HashMap<>(data(content)); data.put("flags", 64); return Map.of("type", 4, "data", data); }
    @PreDestroy void close() { if (executor instanceof ExecutorService service) service.shutdownNow(); }
    private record Draft(OfficialSongSource.Song song, long reportId, String owner, Instant createdAt) {}
    private static final class Operation {
        final String owner;
        final Instant createdAt;
        volatile Map<String, Object> result;
        Operation(String owner, Instant createdAt) { this.owner = owner; this.createdAt = createdAt; }
    }
    @FunctionalInterface interface Reply { void send(JsonNode root, Map<String, Object> data) throws Exception; }
    @FunctionalInterface private interface Work { Map<String, Object> run(); }
}
