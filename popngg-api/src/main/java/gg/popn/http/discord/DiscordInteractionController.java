package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.port.in.CreateSongUseCase;
import gg.popn.application.song.port.in.FindSongsUseCase;
import gg.popn.application.song.port.in.FindSongDetailUseCase;
import gg.popn.application.song.port.in.UpdateSongUseCase;
import gg.popn.application.song.dto.command.UpdateSongCommand;
import gg.popn.application.song.dto.result.SongDetailView;
import gg.popn.application.song.dto.query.FindSongsQuery;
import gg.popn.application.song.port.out.JacketStoragePort;
import gg.popn.application.song.port.out.AdminNotificationPort;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import gg.popn.domain.chart.model.field.SongHashGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/discord/interactions")
public class DiscordInteractionController {
    private static final byte[] ED25519_X509_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");
    private static final int EPHEMERAL = 64;

    private final ObjectMapper mapper;
    private final CreateSongUseCase createSong;
    private final FindSongsUseCase findSongs;
    private final JacketStoragePort jacketStorage;
    private final FindSongDetailUseCase findSongDetail;
    private final UpdateSongUseCase updateSong;
    private final AdminNotificationPort adminNotification;
    private final UnknownChartReportPort unknownChartReport;
    private final JacketDownloader jacketDownloader;
    private final byte[] publicKey;
    private final String guildId;
    private final String adminRoleId;
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Map<String, PreDraft> preDrafts = new ConcurrentHashMap<>();
    private final Map<String, EditDraft> editDrafts = new ConcurrentHashMap<>();

    public DiscordInteractionController(ObjectMapper mapper, CreateSongUseCase createSong,
            FindSongsUseCase findSongs,
            JacketStoragePort jacketStorage,
            FindSongDetailUseCase findSongDetail, UpdateSongUseCase updateSong,
            AdminNotificationPort adminNotification,
            UnknownChartReportPort unknownChartReport,
            @Value("${popngg.discord.public-key:}") String publicKey,
            @Value("${popngg.discord.guild-id:}") String guildId,
            @Value("${popngg.discord.admin-role-id:}") String adminRoleId) {
        this(mapper, createSong, findSongs, jacketStorage, findSongDetail, updateSong,
                adminNotification, unknownChartReport, publicKey, guildId, adminRoleId,
                DiscordInteractionController::downloadPng);
    }

    DiscordInteractionController(ObjectMapper mapper, CreateSongUseCase createSong,
            FindSongsUseCase findSongs, JacketStoragePort jacketStorage,
            FindSongDetailUseCase findSongDetail, UpdateSongUseCase updateSong,
            AdminNotificationPort adminNotification, UnknownChartReportPort unknownChartReport,
            String publicKey, String guildId, String adminRoleId, JacketDownloader jacketDownloader) {
        this.mapper = mapper;
        this.createSong = createSong;
        this.findSongs = findSongs;
        this.jacketStorage = jacketStorage;
        this.findSongDetail = findSongDetail;
        this.updateSong = updateSong;
        this.adminNotification = adminNotification;
        this.unknownChartReport = unknownChartReport;
        this.publicKey = publicKey.isBlank() ? new byte[0] : HexFormat.of().parseHex(publicKey.strip());
        this.guildId = guildId;
        this.adminRoleId = adminRoleId;
        this.jacketDownloader = jacketDownloader;
    }

    @PostMapping
    public ResponseEntity<?> interact(@RequestBody byte[] body,
            @RequestHeader("X-Signature-Ed25519") String signature,
            @RequestHeader("X-Signature-Timestamp") String timestamp) throws Exception {
        if (!validSignature(body, signature, timestamp)) return ResponseEntity.status(401).build();
        JsonNode root = mapper.readTree(body);
        cleanupDrafts();
        int type = root.path("type").asInt();
        if (type == 1) return ResponseEntity.ok(Map.of("type", 1));
        if (!authorized(root)) return ResponseEntity.ok(message("관리자 역할이 필요합니다."));
        if (type == 2 && "곡추가".equals(root.path("data").path("name").asText())) {
            try {
                JsonNode attachmentOption = option(root, "자켓");
                String attachmentId = attachmentOption.path("value").asText();
                JsonNode attachment = root.path("data").path("resolved").path("attachments").path(attachmentId);
                if (attachment.path("size").asLong() > 5 * 1024 * 1024L) {
                    return ResponseEntity.ok(message("자켓 이미지는 5MB 이하여야 합니다."));
                }
                String contentType = attachment.path("content_type").asText();
                if (!contentType.startsWith("image/")) return ResponseEntity.ok(message("이미지 파일만 첨부할 수 있습니다."));
                Instant createdAt = LocalDate.parse(option(root, "추가일").path("value").asText())
                        .atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
                String id = UUID.randomUUID().toString();
                preDrafts.put(id, new PreDraft(attachment.path("url").asText(), createdAt, Instant.now()));
                return ResponseEntity.ok(songModal(id));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("추가일은 YYYY-MM-DD 형식으로 입력해 주세요."));
            }
        }
        if (type == 2 && "곡조회".equals(root.path("data").path("name").asText())) {
            String keyword = root.path("data").path("options").path(0).path("value").asText();
            var page = findSongs.execute(new FindSongsQuery(keyword, null, null, null,
                    null, null, null, null, 0, 10));
            if (page.content().isEmpty()) return ResponseEntity.ok(message("검색 결과가 없습니다."));
            String result = page.content().stream().map(song -> "- `#%d` **%s** / %s / %s (v%d)".formatted(
                            song.songId(), song.songName(), song.genreName(),
                            song.artistName() == null ? "-" : song.artistName(), song.version()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            return ResponseEntity.ok(message("검색 결과 %d건\n%s".formatted(page.totalElements(), result)));
        }
        if (type == 2 && "미등록목록".equals(root.path("data").path("name").asText())) {
            var reports = unknownChartReport.findRecentUnresolved(20);
            if (reports.isEmpty()) return ResponseEntity.ok(message("현재 미등록 곡/채보가 없습니다."));
            String content = reports.stream().map(report ->
                    "- `#%d` **%s** / %s / 난이도 %d / UPPER %s / %d회".formatted(
                            report.reportId(), report.songName(), report.genreName(),
                            report.difficultyCode(), report.upper(), report.occurrences()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            return ResponseEntity.ok(message("**최근 미등록 곡/채보**\n" + content));
        }
        if (type == 2 && "곡수정".equals(root.path("data").path("name").asText())) {
            try {
                long songId = option(root, "song_id").path("value").asLong();
                SongDetailView current = findSongDetail.findSong(songId);
                JsonNode dateOption = optionalOption(root, "추가일");
                Instant createdAt = dateOption == null ? null : LocalDate.parse(dateOption.path("value").asText())
                        .atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
                String attachmentUrl = null;
                JsonNode jacketOption = optionalOption(root, "자켓");
                if (jacketOption != null) {
                    JsonNode attachment = root.path("data").path("resolved").path("attachments")
                            .path(jacketOption.path("value").asText());
                    if (attachment.path("size").asLong() > 5 * 1024 * 1024L
                            || !attachment.path("content_type").asText().startsWith("image/"))
                        return ResponseEntity.ok(message("자켓은 5MB 이하 이미지여야 합니다."));
                    attachmentUrl = attachment.path("url").asText();
                }
                String id = UUID.randomUUID().toString();
                editDrafts.put(id, new EditDraft(current, null, attachmentUrl, createdAt, Instant.now()));
                return ResponseEntity.ok(editModal(id, current));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("곡을 찾을 수 없거나 추가일 형식이 올바르지 않습니다."));
            }
        }
        if (type == 5 && root.path("data").path("custom_id").asText().startsWith("song_edit:")) {
            String editId = root.path("data").path("custom_id").asText().substring("song_edit:".length());
            EditDraft stored = editDrafts.remove(editId);
            if (stored == null) return ResponseEntity.ok(message("수정 요청이 만료되었습니다."));
            try {
                Map<String, String> values = modalValues(root);
                List<UpdateSongCommand.ChartUpdate> charts = parseChartUpdates(values.get("charts"), stored.current());
                var command = new UpdateSongCommand(stored.current().song().songId(), values.get("genre"),
                        values.get("song"), values.get("artist"), Integer.parseInt(values.get("version")),
                        null, stored.requestedCreatedAt(), charts);
                String confirmId = UUID.randomUUID().toString();
                editDrafts.put(confirmId, new EditDraft(stored.current(), command, stored.attachmentUrl(),
                        stored.requestedCreatedAt(), Instant.now()));
                return ResponseEntity.ok(editPreview(confirmId, stored.current(), command));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("수정 입력 오류: " + exception.getMessage()));
            }
        }
        if (type == 3 && root.path("data").path("custom_id").asText().startsWith("song_edit_confirm:")) {
            String id = root.path("data").path("custom_id").asText().substring("song_edit_confirm:".length());
            EditDraft edit = editDrafts.remove(id);
            if (edit == null || edit.command() == null) return ResponseEntity.ok(message("수정 요청이 만료되었습니다."));
            try {
                UpdateSongCommand command = edit.command();
                String oldHash = edit.current().song().songHash();
                boolean upper = command.charts().isEmpty() ? edit.current().charts().getFirst().isUpper()
                        : command.charts().getFirst().isUpper();
                String newHash = SongHashGenerator.generate(command.genreName(), command.songName(),
                        command.artistName(), command.version(), upper);
                String backupKey = null;
                boolean newObject = false;
                if (edit.attachmentUrl() != null) {
                    byte[] png = jacketDownloader.download(edit.attachmentUrl());
                    String jacketUrl;
                    if (newHash.equals(oldHash)) {
                        backupKey = jacketStorage.replacePng(oldHash, png);
                        jacketUrl = edit.current().song().jacketUrl();
                    } else {
                        jacketUrl = jacketStorage.uploadPng(newHash, png);
                        newObject = true;
                    }
                    command = new UpdateSongCommand(command.songId(), command.genreName(), command.songName(),
                            command.artistName(), command.version(), jacketUrl, command.createdAt(), command.charts());
                }
                try {
                    SongDetailView updated = updateSong.execute(command);
                    adminNotification.send("**[곡 수정]** 관리자: `<@%s>` / songId: `%d` / 곡명: **%s** / songHash: `%s`".formatted(
                            actorId(root), updated.song().songId(), updated.song().songName(), updated.song().songHash()));
                    return ResponseEntity.ok(message("곡 수정 완료: `#%d` **%s**\n새 songHash: `%s`".formatted(
                            updated.song().songId(), updated.song().songName(), updated.song().songHash())));
                } catch (RuntimeException exception) {
                    if (newObject) jacketStorage.delete(newHash);
                    if (backupKey != null) jacketStorage.restore(oldHash, backupKey);
                    throw exception;
                }
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("곡 수정 실패: " + exception.getMessage()));
            }
        }
        if (type == 5 && root.path("data").path("custom_id").asText().startsWith("song_create:")) {
            try {
                String preId = root.path("data").path("custom_id").asText().substring("song_create:".length());
                PreDraft preDraft = preDrafts.remove(preId);
                if (preDraft == null) return ResponseEntity.ok(message("입력 요청이 만료되었습니다."));
                Draft draft = draft(root, preDraft);
                String id = UUID.randomUUID().toString();
                drafts.put(id, draft);
                return ResponseEntity.ok(preview(id, draft));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("입력 형식이 올바르지 않습니다: " + exception.getMessage()));
            }
        }
        if (type == 3 && root.path("data").path("custom_id").asText().startsWith("song_confirm:")) {
            String id = root.path("data").path("custom_id").asText().substring("song_confirm:".length());
            Draft draft = drafts.remove(id);
            if (draft == null || draft.createdAt().isBefore(Instant.now().minusSeconds(900))) {
                return ResponseEntity.ok(message("등록 요청이 만료되었습니다. `/곡추가`를 다시 실행해 주세요."));
            }
            try {
                boolean upper = draft.command().charts().getFirst().isUpper();
                String songHash = SongHashGenerator.generate(draft.command().genreName(),
                        draft.command().songName(), draft.command().artistName(),
                        draft.command().version(), upper);
                byte[] png = jacketDownloader.download(draft.attachmentUrl());
                String jacketUrl = jacketStorage.uploadPng(songHash, png);
                var command = new CreateSongCommand(songHash, draft.command().genreName(),
                        draft.command().songName(), draft.command().artistName(), draft.command().version(),
                        jacketUrl, draft.command().createdAt(), draft.command().charts());
                try {
                    var result = createSong.execute(command);
                    adminNotification.send("**[곡 추가]** 관리자: `<@%s>` / songId: `%d` / 곡명: **%s** / songHash: `%s`".formatted(
                            actorId(root), result.songId(), command.songName(), songHash));
                    return ResponseEntity.ok(message("곡 등록 완료: **%s** (`songId=%d`, 채보 %d개)".formatted(
                            command.songName(), result.songId(), result.chartIds().size())));
                } catch (RuntimeException exception) {
                    jacketStorage.delete(songHash);
                    throw exception;
                }
            } catch (Exception exception) {
                return ResponseEntity.ok(message("곡 등록 실패: " + exception.getMessage()));
            }
        }
        if (type == 3 && "song_cancel".equals(root.path("data").path("custom_id").asText())) {
            return ResponseEntity.ok(message("곡 등록을 취소했습니다."));
        }
        return ResponseEntity.ok(message("지원하지 않는 명령입니다."));
    }

    private boolean validSignature(byte[] body, String signatureHex, String timestamp) {
        if (publicKey.length != 32) return false;
        try {
            long requestEpochSeconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - requestEpochSeconds) > 300) return false;
            byte[] encoded = new byte[ED25519_X509_PREFIX.length + publicKey.length];
            System.arraycopy(ED25519_X509_PREFIX, 0, encoded, 0, ED25519_X509_PREFIX.length);
            System.arraycopy(publicKey, 0, encoded, ED25519_X509_PREFIX.length, publicKey.length);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded)));
            verifier.update(timestamp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            verifier.update(body);
            return verifier.verify(HexFormat.of().parseHex(signatureHex));
        } catch (Exception ignored) { return false; }
    }

    private void cleanupDrafts() {
        Instant cutoff = Instant.now().minusSeconds(900);
        drafts.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
        preDrafts.entrySet().removeIf(entry -> entry.getValue().requestedAt().isBefore(cutoff));
        editDrafts.entrySet().removeIf(entry -> entry.getValue().requestedAt().isBefore(cutoff));
    }

    private boolean authorized(JsonNode root) {
        if (!guildId.equals(root.path("guild_id").asText())) return false;
        for (JsonNode role : root.path("member").path("roles")) {
            if (adminRoleId.equals(role.asText())) return true;
        }
        return false;
    }

    private static Map<String, Object> songModal(String id) {
        return Map.of("type", 9, "data", Map.of("custom_id", "song_create:" + id, "title", "곡 추가",
                "components", List.of(input("song", "곡명", "곡명을 입력하세요"),
                        input("genre", "장르", "장르를 입력하세요"),
                        input("artist", "아티스트", "아티스트를 입력하세요"),
                        input("version", "버전", "예: 29"),
                        input("charts", "채보", "예: N:30,H:42,EX:48 또는 UPPER H:45,EX:50"))));
    }

    private static Map<String, Object> input(String id, String label, String placeholder) {
        return Map.of("type", 1, "components", List.of(Map.of("type", 4, "custom_id", id,
                "label", label, "style", 1, "required", true, "placeholder", placeholder)));
    }

    private static Map<String, Object> inputValue(String id, String label, String value) {
        return Map.of("type", 1, "components", List.of(Map.of("type", 4, "custom_id", id,
                "label", label, "style", 1, "required", true, "value", value == null ? "" : value)));
    }

    private static Map<String, Object> editModal(String id, SongDetailView current) {
        String charts = current.charts().stream().filter(chart -> !chart.isDeleted())
                .map(chart -> chart.difficulty().shortLabel() + ":" + chart.level())
                .collect(java.util.stream.Collectors.joining(","));
        if (!current.charts().isEmpty() && current.charts().getFirst().isUpper()) charts = "UPPER " + charts;
        return Map.of("type", 9, "data", Map.of("custom_id", "song_edit:" + id, "title", "곡 수정",
                "components", List.of(inputValue("song", "곡명", current.song().songName()),
                        inputValue("genre", "장르", current.song().genreName()),
                        inputValue("artist", "아티스트", current.song().artistName()),
                        inputValue("version", "버전", Integer.toString(current.song().version())),
                        inputValue("charts", "채보", charts))));
    }

    private static Map<String, String> modalValues(JsonNode root) {
        Map<String, String> values = new java.util.HashMap<>();
        for (JsonNode row : root.path("data").path("components")) {
            JsonNode component = row.path("components").path(0);
            values.put(component.path("custom_id").asText(), component.path("value").asText().strip());
        }
        return values;
    }

    private static List<UpdateSongCommand.ChartUpdate> parseChartUpdates(String spec, SongDetailView current) {
        boolean upper = spec.toUpperCase().startsWith("UPPER ");
        if (upper) spec = spec.substring(6).strip();
        Map<Integer, Integer> levels = new java.util.HashMap<>();
        for (String item : spec.split(",")) {
            String[] pair = item.strip().split(":", 2);
            int code = switch (pair[0].strip().toUpperCase()) {
                case "E", "EASY", "L", "LIGHT" -> 1; case "N" -> 2; case "H" -> 3; case "EX" -> 4;
                default -> throw new IllegalArgumentException("지원하지 않는 난이도: " + pair[0]);
            };
            levels.put(code, Integer.parseInt(pair[1].strip()));
        }
        return current.charts().stream().filter(chart -> levels.containsKey(chart.difficulty().code()))
                .map(chart -> new UpdateSongCommand.ChartUpdate(chart.chartId(),
                        levels.get(chart.difficulty().code()), chart.chartVersion(), upper,
                        chart.hasStrictGauge(), chart.hasStrictJudgement())).toList();
    }

    private static Map<String, Object> editPreview(String id, SongDetailView before, UpdateSongCommand after) {
        return Map.of("type", 4, "data", Map.of("flags", EPHEMERAL,
                "content", "**곡 수정 확인**\n`#%d` %s → **%s**\n장르: %s → %s\n아티스트: %s → %s\n버전: %d → %d\n채보 수정: %d개".formatted(
                        before.song().songId(), before.song().songName(), after.songName(),
                        before.song().genreName(), after.genreName(), before.song().artistName(),
                        after.artistName(), before.song().version(), after.version(), after.charts().size()),
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 3, "label", "수정 확정", "custom_id", "song_edit_confirm:" + id),
                        Map.of("type", 2, "style", 4, "label", "취소", "custom_id", "song_cancel"))))));
    }

    private Draft draft(JsonNode root, PreDraft preDraft) {
        Map<String, String> values = new java.util.HashMap<>();
        for (JsonNode row : root.path("data").path("components")) {
            JsonNode component = row.path("components").path(0);
            values.put(component.path("custom_id").asText(), component.path("value").asText().strip());
        }
        int version = Integer.parseInt(values.get("version"));
        String spec = values.get("charts");
        boolean upper = spec.toUpperCase().startsWith("UPPER ");
        if (upper) spec = spec.substring(6).strip();
        List<CreateSongCommand.CreateChartCommand> charts = new ArrayList<>();
        for (String item : spec.split(",")) {
            String[] pair = item.strip().split(":", 2);
            int difficulty = switch (pair[0].strip().toUpperCase()) {
                case "E", "EASY", "L", "LIGHT" -> 1; case "N" -> 2;
                case "H" -> 3; case "EX" -> 4;
                default -> throw new IllegalArgumentException("지원하지 않는 난이도: " + pair[0]);
            };
            charts.add(new CreateSongCommand.CreateChartCommand(difficulty,
                    Integer.parseInt(pair[1].strip()), version, upper, false, false));
        }
        return new Draft(new CreateSongCommand(null, values.get("genre"), values.get("song"),
                values.get("artist"), version, null, preDraft.createdAt(), List.copyOf(charts)),
                preDraft.attachmentUrl(), Instant.now());
    }

    private static Map<String, Object> preview(String id, Draft draft) {
        String charts = draft.command().charts().stream()
                .map(c -> "%d:%d".formatted(c.difficulty(), c.level()))
                .collect(java.util.stream.Collectors.joining(", "));
        return Map.of("type", 4, "data", Map.of("flags", EPHEMERAL,
                "content", "**등록 확인**\n곡명: %s\n장르: %s\n아티스트: %s\n버전: %d\n채보: %s".formatted(
                        draft.command().songName(), draft.command().genreName(), draft.command().artistName(),
                        draft.command().version(), charts),
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 3, "label", "등록", "custom_id", "song_confirm:" + id),
                        Map.of("type", 2, "style", 4, "label", "취소", "custom_id", "song_cancel"))))));
    }

    private static Map<String, Object> message(String content) {
        return Map.of("type", 4, "data", Map.of("flags", EPHEMERAL, "content", content));
    }

    private static JsonNode option(JsonNode root, String name) {
        for (JsonNode option : root.path("data").path("options")) {
            if (name.equals(option.path("name").asText())) return option;
        }
        throw new IllegalArgumentException("Missing option: " + name);
    }

    private static JsonNode optionalOption(JsonNode root, String name) {
        for (JsonNode option : root.path("data").path("options")) {
            if (name.equals(option.path("name").asText())) return option;
        }
        return null;
    }

    private static String actorId(JsonNode root) {
        return root.path("member").path("user").path("id").asText("unknown");
    }

    private static byte[] downloadPng(String url) throws Exception {
        URI uri = URI.create(url);
        if (!"https".equals(uri.getScheme()) || !("cdn.discordapp.com".equals(uri.getHost())
                || "media.discordapp.net".equals(uri.getHost()))) {
            throw new IllegalArgumentException("허용되지 않은 자켓 URL입니다.");
        }
        byte[] source = HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri)
                        .timeout(java.time.Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray()).body();
        if (source.length > 5 * 1024 * 1024) throw new IllegalArgumentException("자켓 이미지는 5MB 이하여야 합니다.");
        var image = ImageIO.read(new ByteArrayInputStream(source));
        if (image == null) throw new IllegalArgumentException("올바른 이미지가 아닙니다.");
        if (image.getWidth() != image.getHeight()) throw new IllegalArgumentException("자켓 이미지는 정사각형이어야 합니다.");
        if ((long) image.getWidth() * image.getHeight() > 16_777_216L) throw new IllegalArgumentException("이미지 해상도가 너무 큽니다.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IllegalArgumentException("PNG 변환에 실패했습니다.");
        byte[] png = output.toByteArray();
        if (png.length > 5 * 1024 * 1024) throw new IllegalArgumentException("변환된 PNG가 5MB를 초과합니다.");
        return png;
    }

    private record PreDraft(String attachmentUrl, Instant createdAt, Instant requestedAt) {}
    private record Draft(CreateSongCommand command, String attachmentUrl, Instant createdAt) {}
    private record EditDraft(SongDetailView current, UpdateSongCommand command, String attachmentUrl,
                             Instant requestedCreatedAt, Instant requestedAt) {}
    @FunctionalInterface interface JacketDownloader { byte[] download(String url) throws Exception; }
}
