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
import gg.popn.application.account.port.in.AdminPasswordResetUseCase;
import gg.popn.application.common.ErrorNotificationPort;
import gg.popn.domain.chart.model.field.SongHashGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private static final Logger log = LoggerFactory.getLogger(DiscordInteractionController.class);

    private final ObjectMapper mapper;
    private final CreateSongUseCase createSong;
    private final FindSongsUseCase findSongs;
    private final JacketStoragePort jacketStorage;
    private final FindSongDetailUseCase findSongDetail;
    private final UpdateSongUseCase updateSong;
    private final AdminNotificationPort adminNotification;
    private final UnknownChartReportPort unknownChartReport;
    private final AdminPasswordResetUseCase adminPasswordReset;
    private final DeploymentVersion deploymentVersion;
    private final ErrorNotificationPort errorNotification;
    private final PerformanceDiagnostics performanceDiagnostics;
    private final IncidentThreadTestClient incidentThreadTestClient;
    private final String grafanaUrl;
    private final JacketDownloader jacketDownloader;
    private final byte[] publicKey;
    private final String guildId;
    private final String adminRoleId;
    private final Map<String, Draft> drafts = new ConcurrentHashMap<>();
    private final Map<String, PreDraft> preDrafts = new ConcurrentHashMap<>();
    private final Map<String, EditDraft> editDrafts = new ConcurrentHashMap<>();

    @Autowired
    public DiscordInteractionController(ObjectMapper mapper, CreateSongUseCase createSong,
            FindSongsUseCase findSongs,
            JacketStoragePort jacketStorage,
            FindSongDetailUseCase findSongDetail, UpdateSongUseCase updateSong,
            AdminNotificationPort adminNotification,
            UnknownChartReportPort unknownChartReport,
            AdminPasswordResetUseCase adminPasswordReset,
            DeploymentVersion deploymentVersion,
            ErrorNotificationPort errorNotification,
            PerformanceDiagnostics performanceDiagnostics,
            IncidentThreadTestClient incidentThreadTestClient,
            @Value("${popngg.discord.public-key:}") String publicKey,
            @Value("${popngg.discord.guild-id:}") String guildId,
            @Value("${popngg.discord.admin-role-id:}") String adminRoleId,
            @Value("${popngg.monitoring.grafana-url:https://grafana.popn.gg}") String grafanaUrl) {
        this(mapper, createSong, findSongs, jacketStorage, findSongDetail, updateSong,
                adminNotification, unknownChartReport, adminPasswordReset, deploymentVersion,
                errorNotification, performanceDiagnostics, incidentThreadTestClient,
                publicKey, guildId, adminRoleId, grafanaUrl,
                DiscordInteractionController::downloadPng);
    }

    DiscordInteractionController(ObjectMapper mapper, CreateSongUseCase createSong,
            FindSongsUseCase findSongs, JacketStoragePort jacketStorage,
            FindSongDetailUseCase findSongDetail, UpdateSongUseCase updateSong,
            AdminNotificationPort adminNotification, UnknownChartReportPort unknownChartReport,
            AdminPasswordResetUseCase adminPasswordReset,
            DeploymentVersion deploymentVersion,
            ErrorNotificationPort errorNotification,
            PerformanceDiagnostics performanceDiagnostics,
            IncidentThreadTestClient incidentThreadTestClient,
            String publicKey, String guildId, String adminRoleId, String grafanaUrl,
            JacketDownloader jacketDownloader) {
        this.mapper = mapper;
        this.createSong = createSong;
        this.findSongs = findSongs;
        this.jacketStorage = jacketStorage;
        this.findSongDetail = findSongDetail;
        this.updateSong = updateSong;
        this.adminNotification = adminNotification;
        this.unknownChartReport = unknownChartReport;
        this.adminPasswordReset = adminPasswordReset;
        this.deploymentVersion = deploymentVersion;
        this.errorNotification = errorNotification;
        this.performanceDiagnostics = performanceDiagnostics;
        this.incidentThreadTestClient = incidentThreadTestClient;
        this.publicKey = publicKey.isBlank() ? new byte[0] : HexFormat.of().parseHex(publicKey.strip());
        this.guildId = guildId;
        this.adminRoleId = adminRoleId;
        this.grafanaUrl = stripTrailingSlash(grafanaUrl);
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
        if (type == 2 && "배포버전".equals(root.path("data").path("name").asText())) {
            return ResponseEntity.ok(ephemeral(deploymentVersion.message()));
        }
        if (type == 2 && "성능대시보드".equals(root.path("data").path("name").asText())) {
            PerformanceDiagnostics.Snapshot snapshot = performanceDiagnostics.snapshot();
            return ResponseEntity.ok(ephemeral(performanceMessage(snapshot, false)));
        }
        if (type == 2 && "장애상태확인".equals(root.path("data").path("name").asText())) {
            PerformanceDiagnostics.Snapshot snapshot = performanceDiagnostics.snapshot();
            return ResponseEntity.ok(ephemeral(performanceMessage(snapshot, true)));
        }
        if (type == 2 && "장애알림테스트".equals(root.path("data").path("name").asText())) {
            boolean accepted = incidentThreadTestClient.requestTest();
            String result = accepted
                    ? "**장애 알림 스레드 테스트를 요청했습니다.**\nerror-log 채널에 테스트 부모 메시지와 스레드가 생성되는지 확인해 주세요."
                    : "**장애 알림 테스트 요청에 실패했습니다.**\nincident-bot 상태와 Discord Bot 권한을 확인해 주세요.";
            return ResponseEntity.ok(ephemeral(result));
        }
        if (type == 2 && "에러알림테스트".equals(root.path("data").path("name").asText())) {
            String traceId = "diagnostic-" + UUID.randomUUID();
            log.warn("Synthetic Discord error notification test. traceId={}, requestedBy={}",
                    traceId, actorId(root));
            errorNotification.notifyServerError("SYNTHETIC", "/diagnostics/discord",
                    "DiagnosticTestException", "관리자 요청으로 생성된 오류 알림 테스트입니다.",
                    "-", traceId);
            return ResponseEntity.ok(ephemeral("**API 에러 알림 테스트**를 error-log Webhook으로 전송했습니다.\n"
                    + "이 테스트는 장애 감지나 스레드 생성을 검증하지 않습니다.\n추적 ID: `" + traceId
                    + "`\n같은 테스트 알림은 5분 동안 중복 억제됩니다."));
        }
        if (type == 2 && "비밀번호초기화".equals(root.path("data").path("name").asText())) {
            String poptomoId = option(root, "팝토모_id").path("value").asText();
            String temporaryPassword;
            try {
                temporaryPassword = adminPasswordReset.reset(poptomoId);
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(ephemeral("팝토모 ID를 확인할 수 없거나 사용자를 찾지 못했습니다."));
            }
            try {
                adminNotification.send("[Discord 관리자] 비밀번호 초기화\n대상: `" + poptomoId + "`");
            } catch (RuntimeException ignored) {
                // The password is already changed; a notification outage must not hide the temporary password.
            }
            return ResponseEntity.ok(ephemeral("**비밀번호 초기화 완료**\n대상: `" + poptomoId
                    + "`\n임시 비밀번호: `" + temporaryPassword
                    + "`\n\n이 비밀번호는 다시 표시되지 않습니다. 사용자에게 안전하게 전달해 주세요."));
        }
        if (type == 2 && "곡추가".equals(root.path("data").path("name").asText())) {
            try {
                Draft draft = createDraftFromOptions(root);
                String id = UUID.randomUUID().toString();
                drafts.put(id, draft);
                return ResponseEntity.ok(preview(id, draft));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("곡 추가 입력 오류: " + exception.getMessage()));
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
                    "- `#%d` **%s** %s / %s / %s / %d회".formatted(
                            report.reportId(), report.songName(),
                            report.missingVariant()
                                    ? report.upper() ? "[UPPER 누락]" : "[일반 버전 누락]"
                                    : "[곡 미등록]",
                            report.genreName(), report.artistName(), report.occurrences()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            List<Map<String, Object>> choices = reports.stream().limit(25).map(report -> Map.<String, Object>of(
                    "label", truncate(report.songName(), 100), "description", truncate(report.genreName(), 100),
                    "value", Long.toString(report.reportId()))).toList();
            return ResponseEntity.ok(Map.of("type", 4, "data", Map.of(
                    "content", "**최근 미등록 곡/채보**\n" + content + "\n아래에서 선택하면 곡 추가 창이 열립니다.",
                    "components", List.of(Map.of("type", 1, "components", List.of(Map.of(
                            "type", 3, "custom_id", "unknown_song_select", "placeholder", "추가할 곡 선택",
                            "min_values", 1, "max_values", 1, "options", choices)))))));
        }
        if (type == 2 && "정보보완목록".equals(root.path("data").path("name").asText())) {
            var reports = unknownChartReport.findRecentIncomplete(20);
            if (reports.isEmpty()) return ResponseEntity.ok(message("현재 정보 보완이 필요한 곡이 없습니다."));
            String content = reports.stream().map(report ->
                    "- `#%d` **%s** / %s / 등록: %s / 감지: %s / %d회".formatted(
                            report.reportId(), report.songName(), report.genreName(),
                            truncate(report.registeredArtistName(), 60),
                            truncate(report.reportedArtistName(), 60), report.occurrences()))
                    .collect(java.util.stream.Collectors.joining("\n"));
            List<Map<String, Object>> choices = reports.stream().limit(25).map(report -> Map.<String, Object>of(
                    "label", truncate(report.songName(), 100),
                    "description", truncate("등록: " + report.registeredArtistName() + " / 감지: "
                            + report.reportedArtistName(), 100),
                    "value", Long.toString(report.reportId()))).toList();
            return ResponseEntity.ok(Map.of("type", 4, "data", Map.of(
                    "content", "**곡 정보 보완 목록**\n" + content + "\n아래에서 선택하면 기존 곡 수정 창이 열립니다.",
                    "components", List.of(Map.of("type", 1, "components", List.of(Map.of(
                            "type", 3, "custom_id", "incomplete_song_select", "placeholder", "보완할 곡 선택",
                            "min_values", 1, "max_values", 1, "options", choices)))))));
        }
        if (type == 3 && "incomplete_song_select".equals(root.path("data").path("custom_id").asText())) {
            long reportId = root.path("data").path("values").path(0).asLong();
            var selected = unknownChartReport.findRecentIncomplete(100).stream()
                    .filter(report -> report.reportId() == reportId).findFirst();
            if (selected.isEmpty()) return ResponseEntity.ok(message("정보 보완 항목을 찾을 수 없습니다."));
            SongDetailView current = findSongDetail.findSong(selected.get().songId());
            String id = UUID.randomUUID().toString();
            editDrafts.put(id, new EditDraft(current, null, null, null, Instant.now(), reportId));
            return ResponseEntity.ok(editModal(id, current, null));
        }
        if (type == 3 && "unknown_song_select".equals(root.path("data").path("custom_id").asText())) {
            long reportId = root.path("data").path("values").path(0).asLong();
            var selected = unknownChartReport.findRecentUnresolved(100).stream()
                    .filter(report -> report.reportId() == reportId).findFirst();
            if (selected.isEmpty()) return ResponseEntity.ok(message("미등록 곡 정보를 찾을 수 없습니다."));
            var report = selected.get();
            String id = UUID.randomUUID().toString();
            Prefill prefill = new Prefill(report.songName(), report.genreName(), report.artistName(),
                    Boolean.TRUE.equals(report.upper()));
            preDrafts.put(id, new PreDraft(prefill, Instant.now()));
            return ResponseEntity.ok(unknownSongModal(id, prefill));
        }
        if (type == 2 && "곡수정".equals(root.path("data").path("name").asText())) {
            try {
                long songId = option(root, "song_id").path("value").asLong();
                SongDetailView current = findSongDetail.findSong(songId);
                if (!hasSongUpdateOptions(root)) {
                    String id = UUID.randomUUID().toString();
                    editDrafts.put(id, new EditDraft(
                            current, null, null, null, Instant.now(), null));
                    return ResponseEntity.ok(editModal(id, current, null));
                }
                Instant createdAt = optionalDate(root, "추가일");
                String attachmentUrl = optionalAttachmentUrl(root, "자켓");
                UpdateSongCommand command = updateCommandFromOptions(root, current, createdAt);
                String id = UUID.randomUUID().toString();
                editDrafts.put(id, new EditDraft(current, command, attachmentUrl, createdAt, Instant.now(), null));
                return ResponseEntity.ok(editPreview(id, current, command));
            } catch (RuntimeException exception) {
                return ResponseEntity.ok(message("곡을 찾을 수 없거나 추가일 형식이 올바르지 않습니다."));
            }
        }
        if (type == 3 && root.path("data").path("custom_id").asText()
                .startsWith("song_edit_reopen:")) {
            String id = root.path("data").path("custom_id").asText()
                    .substring("song_edit_reopen:".length());
            EditDraft stored = editDrafts.remove(id);
            if (stored == null || stored.command() == null)
                return ResponseEntity.ok(message("수정 요청이 만료되었습니다."));
            String nextId = UUID.randomUUID().toString();
            editDrafts.put(nextId, new EditDraft(stored.current(), null,
                    stored.attachmentUrl(), stored.requestedCreatedAt(), Instant.now(),
                    stored.reportId()));
            return ResponseEntity.ok(editModal(nextId, stored.current(), stored.command()));
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
                        stored.requestedCreatedAt(), Instant.now(), stored.reportId()));
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
                    if (edit.reportId() != null) unknownChartReport.resolve(edit.reportId());
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

    private Map<String, Object> unknownSongModal(String id, Prefill prefill) {
        String metadata;
        try {
            metadata = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of(
                    "songName", prefill.song(), "genreName", prefill.genre(),
                    "artistName", prefill.artist(), "upper", prefill.upper()));
        } catch (Exception exception) {
            throw new IllegalStateException("미등록 곡 정보를 만들 수 없습니다.", exception);
        }
        return Map.of("type", 9, "data", Map.of("custom_id", "song_create:" + id, "title", "곡 추가",
                "components", List.of(
                        fileInput("jacket", "자켓"),
                        modernInput("date", "추가일", "YYYY-MM-DD", "", true),
                        modernTextArea("metadata", "곡 기본정보 JSON", metadata),
                        modernInput("version", "버전", "예: 29", "", true),
                        modernInput("levels", "난이도", "대괄호 안에 숫자 입력, 없으면 공백",
                                "L:[], N:[], H:[], EX:[]", true))));
    }

    private static Map<String, Object> fileInput(String id, String label) {
        return Map.of("type", 18, "label", label, "component", Map.of(
                "type", 19, "custom_id", id, "min_values", 1, "max_values", 1, "required", true));
    }

    private static Map<String, Object> modernInput(String id, String label, String placeholder,
                                                    String value, boolean required) {
        Map<String, Object> component = new java.util.LinkedHashMap<>();
        component.put("type", 4); component.put("custom_id", id); component.put("style", 1);
        component.put("required", required); component.put("placeholder", placeholder);
        if (value != null && !value.isBlank()) component.put("value", value);
        return Map.of("type", 18, "label", label, "component", component);
    }

    private static Map<String, Object> modernTextArea(String id, String label, String value) {
        return Map.of("type", 18, "label", label, "component", Map.of(
                "type", 4, "custom_id", id, "style", 2, "required", true, "value", value));
    }

    private static Map<String, Object> input(String id, String label, String placeholder) {
        return Map.of("type", 1, "components", List.of(Map.of("type", 4, "custom_id", id,
                "label", label, "style", 1, "required", true, "placeholder", placeholder)));
    }

    private static Map<String, Object> inputValue(String id, String label, String value) {
        return Map.of("type", 1, "components", List.of(Map.of("type", 4, "custom_id", id,
                "label", label, "style", 1, "required", true, "value", value == null ? "" : value)));
    }

    private static Map<String, Object> editModal(
            String id, SongDetailView current, UpdateSongCommand defaults) {
        List<UpdateSongCommand.ChartUpdate> requested = defaults == null
                ? List.of() : defaults.charts();
        Map<Integer, Integer> requestedLevels = requested.stream().collect(
                java.util.stream.Collectors.toMap(UpdateSongCommand.ChartUpdate::difficultyCode,
                        UpdateSongCommand.ChartUpdate::level));
        String charts = current.charts().stream().filter(chart -> !chart.isDeleted())
                .map(chart -> chart.difficulty().shortLabel() + ":"
                        + requestedLevels.getOrDefault(chart.difficulty().code(), chart.level()))
                .collect(java.util.stream.Collectors.joining(","));
        boolean upper = requested.isEmpty()
                ? !current.charts().isEmpty() && current.charts().getFirst().isUpper()
                : requested.getFirst().isUpper();
        if (upper) charts = "UPPER " + charts;
        String song = defaults == null ? current.song().songName() : defaults.songName();
        String genre = defaults == null ? current.song().genreName() : defaults.genreName();
        String artist = defaults == null ? current.song().artistName() : defaults.artistName();
        int version = defaults == null ? current.song().version() : defaults.version();
        return Map.of("type", 9, "data", Map.of("custom_id", "song_edit:" + id, "title", "곡 수정",
                "components", List.of(inputValue("song", "곡명", song),
                        inputValue("genre", "장르", genre),
                        inputValue("artist", "아티스트", artist),
                        inputValue("version", "버전", Integer.toString(version)),
                        inputValue("charts", "채보", charts))));
    }

    private static boolean hasSongUpdateOptions(JsonNode root) {
        for (JsonNode option : root.path("data").path("options")) {
            if (!"song_id".equals(option.path("name").asText())) return true;
        }
        return false;
    }

    private static Map<String, String> modalValues(JsonNode root) {
        Map<String, String> values = new java.util.HashMap<>();
        for (JsonNode row : root.path("data").path("components")) {
            JsonNode component = row.has("component") ? row.path("component") : row.path("components").path(0);
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
        return levels.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> {
            var existing = current.charts().stream()
                    .filter(chart -> !chart.isDeleted() && chart.difficulty().code() == entry.getKey())
                    .findFirst();
            return existing.<UpdateSongCommand.ChartUpdate>map(chart ->
                    new UpdateSongCommand.ChartUpdate(chart.chartId(), entry.getKey(), entry.getValue(),
                            chart.chartVersion(), upper, chart.hasStrictGauge(), chart.hasStrictJudgement()))
                    .orElseGet(() -> new UpdateSongCommand.ChartUpdate(null, entry.getKey(), entry.getValue(),
                            current.song().version(), upper, false, false));
        }).toList();
    }

    private Draft createDraftFromOptions(JsonNode root) {
        int version = option(root, "버전").path("value").asInt();
        boolean upper = "o".equalsIgnoreCase(option(root, "upper").path("value").asText());
        List<CreateSongCommand.CreateChartCommand> charts = new ArrayList<>();
        String[] names = {"l", "n", "h", "ex"};
        for (int i = 0; i < names.length; i++) {
            JsonNode level = optionalOption(root, names[i]);
            if (level != null) charts.add(new CreateSongCommand.CreateChartCommand(i + 1,
                    level.path("value").asInt(), version, upper, false, false));
        }
        if (charts.isEmpty()) throw new IllegalArgumentException("L/N/H/EX 중 하나 이상 입력해 주세요.");
        Instant date = LocalDate.parse(option(root, "추가일").path("value").asText())
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        String attachmentUrl = requiredAttachmentUrl(root, "자켓");
        var command = new CreateSongCommand(null, optionText(root, "장르"), optionText(root, "곡명"),
                optionText(root, "아티스트"), version, null, date, List.copyOf(charts));
        return new Draft(command, attachmentUrl, Instant.now());
    }

    private UpdateSongCommand updateCommandFromOptions(JsonNode root, SongDetailView current, Instant date) {
        String genre = optionalText(root, "장르", current.song().genreName());
        String song = optionalText(root, "곡명", current.song().songName());
        String artist = optionalText(root, "아티스트", current.song().artistName());
        JsonNode versionOption = optionalOption(root, "버전");
        int version = versionOption == null ? current.song().version() : versionOption.path("value").asInt();
        JsonNode upperOption = optionalOption(root, "upper");
        Boolean upper = upperOption == null ? null : "o".equalsIgnoreCase(upperOption.path("value").asText());
        Map<Integer, Integer> requestedLevels = new java.util.HashMap<>();
        String[] names = {"l", "n", "h", "ex"};
        for (int i = 0; i < names.length; i++) {
            JsonNode level = optionalOption(root, names[i]);
            if (level != null) requestedLevels.put(i + 1, level.path("value").asInt());
        }
        List<UpdateSongCommand.ChartUpdate> charts = current.charts().stream()
                .filter(chart -> !chart.isDeleted()
                        && (requestedLevels.containsKey(chart.difficulty().code()) || upper != null))
                .map(chart -> new UpdateSongCommand.ChartUpdate(chart.chartId(), chart.difficulty().code(),
                        requestedLevels.getOrDefault(chart.difficulty().code(), chart.level()),
                        chart.chartVersion(), upper == null ? chart.isUpper() : upper,
                        chart.hasStrictGauge(), chart.hasStrictJudgement())).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        for (var requested : requestedLevels.entrySet()) {
            if (current.charts().stream().noneMatch(chart -> !chart.isDeleted()
                    && chart.difficulty().code() == requested.getKey())) {
                charts.add(new UpdateSongCommand.ChartUpdate(null, requested.getKey(), requested.getValue(),
                        version, upper == null ? current.charts().getFirst().isUpper() : upper, false, false));
            }
        }
        return new UpdateSongCommand(current.song().songId(), genre, song, artist, version,
                null, date, charts);
    }

    private String requiredAttachmentUrl(JsonNode root, String name) {
        return attachmentUrl(root, option(root, name).path("value").asText());
    }

    private String optionalAttachmentUrl(JsonNode root, String name) {
        JsonNode selected = optionalOption(root, name);
        return selected == null ? null : attachmentUrl(root, selected.path("value").asText());
    }

    private static String attachmentUrl(JsonNode root, String id) {
        JsonNode attachment = root.path("data").path("resolved").path("attachments").path(id);
        if (!attachment.path("content_type").asText().startsWith("image/")
                || attachment.path("size").asLong() > 5 * 1024 * 1024L)
            throw new IllegalArgumentException("자켓은 5MB 이하 이미지여야 합니다.");
        String url = attachment.path("url").asText();
        if (url.isBlank()) throw new IllegalArgumentException("자켓 파일을 찾을 수 없습니다.");
        return url;
    }

    private static Instant optionalDate(JsonNode root, String name) {
        JsonNode value = optionalOption(root, name);
        return value == null ? null : LocalDate.parse(value.path("value").asText())
                .atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static String optionText(JsonNode root, String name) {
        String value = option(root, name).path("value").asText().strip();
        if (value.isBlank()) throw new IllegalArgumentException(name + "은(는) 비워둘 수 없습니다.");
        return value;
    }

    private static String optionalText(JsonNode root, String name, String fallback) {
        JsonNode value = optionalOption(root, name);
        return value == null ? fallback : value.path("value").asText().strip();
    }

    private Map<String, Object> editPreview(String id, SongDetailView before, UpdateSongCommand after) {
        Map<String, Object> json = new java.util.LinkedHashMap<>();
        json.put("songId", after.songId()); json.put("songName", after.songName());
        json.put("genreName", after.genreName()); json.put("artistName", after.artistName());
        json.put("version", after.version());
        if (after.createdAt() != null)
            json.put("date", after.createdAt().atZone(ZoneOffset.UTC).toLocalDate().toString());
        json.put("charts", after.charts().stream().map(chart -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("difficulty", switch (chart.difficultyCode()) {
                case 1 -> "L"; case 2 -> "N"; case 3 -> "H"; case 4 -> "EX"; default -> "-";
            });
            item.put("chartId", chart.chartId());
            item.put("level", chart.level());
            item.put("upper", chart.isUpper());
            return item;
        }).toList());
        String preview;
        try { preview = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json); }
        catch (Exception exception) { throw new IllegalStateException("JSON 미리보기를 만들 수 없습니다.", exception); }
        return Map.of("type", 4, "data", Map.of(
                "content", "**곡 수정 JSON 확인**\n```json\n" + preview + "\n```",
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 3, "label", "수정 확정", "custom_id", "song_edit_confirm:" + id),
                        Map.of("type", 2, "style", 2, "label", "다시 수정", "custom_id", "song_edit_reopen:" + id),
                        Map.of("type", 2, "style", 4, "label", "취소", "custom_id", "song_cancel"))))));
    }

    private Draft draft(JsonNode root, PreDraft preDraft) {
        Map<String, String> values = modalValues(root);
        int version = Integer.parseInt(values.get("version"));
        JsonNode metadata;
        try { metadata = mapper.readTree(values.get("metadata")); }
        catch (Exception exception) { throw new IllegalArgumentException("곡 기본정보 JSON이 올바르지 않습니다."); }
        boolean upper = metadata.path("upper").asBoolean(false);
        List<CreateSongCommand.CreateChartCommand> charts = new ArrayList<>();
        parseLevels(values.get("levels"), version, upper, charts);
        if (charts.isEmpty()) throw new IllegalArgumentException("L/N/H/EX 중 하나 이상 입력해 주세요.");
        JsonNode upload = findModalComponent(root, "jacket");
        String attachmentId = upload.path("values").path(0).asText();
        JsonNode attachment = root.path("data").path("resolved").path("attachments").path(attachmentId);
        if (!attachment.path("content_type").asText().startsWith("image/")
                || attachment.path("size").asLong() > 5 * 1024 * 1024L)
            throw new IllegalArgumentException("자켓은 5MB 이하 이미지여야 합니다.");
        Instant createdAt = LocalDate.parse(values.get("date"))
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        return new Draft(new CreateSongCommand(null, requiredText(metadata, "genreName"),
                requiredText(metadata, "songName"), requiredText(metadata, "artistName"),
                version, null, createdAt, List.copyOf(charts)),
                attachment.path("url").asText(), Instant.now());
    }

    private static void parseLevels(String spec, int version, boolean upper,
                                    List<CreateSongCommand.CreateChartCommand> charts) {
        for (String item : spec.split(",")) {
            if (item.isBlank()) continue;
            String[] pair = item.strip().split(":", 2);
            if (pair.length != 2 || pair[1].isBlank()) continue;
            String level = pair[1].strip();
            if (level.startsWith("[") && level.endsWith("]"))
                level = level.substring(1, level.length() - 1).strip();
            if (level.isBlank()) continue;
            int difficulty = switch (pair[0].strip().toUpperCase()) {
                case "E", "EASY", "L", "LIGHT" -> 1;
                case "N" -> 2; case "H" -> 3; case "EX" -> 4;
                default -> throw new IllegalArgumentException("지원하지 않는 난이도: " + pair[0]);
            };
            charts.add(new CreateSongCommand.CreateChartCommand(difficulty,
                    Integer.parseInt(level), version, upper, false, false));
        }
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText().strip();
        if (value.isBlank()) throw new IllegalArgumentException(field + "은(는) 비워둘 수 없습니다.");
        return value;
    }

    private Map<String, Object> preview(String id, Draft draft) {
        Map<String, Object> json = new java.util.LinkedHashMap<>();
        json.put("songName", draft.command().songName()); json.put("genreName", draft.command().genreName());
        json.put("artistName", draft.command().artistName()); json.put("version", draft.command().version());
        json.put("date", draft.command().createdAt().atZone(ZoneOffset.UTC).toLocalDate().toString());
        json.put("upper", draft.command().charts().getFirst().isUpper());
        Map<String, Integer> levels = new java.util.LinkedHashMap<>();
        String[] names = {"L", "N", "H", "EX"};
        draft.command().charts().forEach(chart -> levels.put(names[chart.difficulty() - 1], chart.level()));
        json.put("levels", levels); json.put("jacket", "첨부됨");
        String preview;
        try { preview = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json); }
        catch (Exception exception) { throw new IllegalStateException("JSON 미리보기를 만들 수 없습니다.", exception); }
        return Map.of("type", 4, "data", Map.of(
                "content", "**곡 등록 JSON 확인**\n```json\n" + preview + "\n```",
                "components", List.of(Map.of("type", 1, "components", List.of(
                        Map.of("type", 2, "style", 3, "label", "등록", "custom_id", "song_confirm:" + id),
                        Map.of("type", 2, "style", 4, "label", "취소", "custom_id", "song_cancel"))))));
    }

    private static Map<String, Object> message(String content) {
        return Map.of("type", 4, "data", Map.of("content", content));
    }

    private static Map<String, Object> ephemeral(String content) {
        return Map.of("type", 4, "data", Map.of("content", content, "flags", 64));
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

    private static JsonNode findModalComponent(JsonNode root, String id) {
        for (JsonNode row : root.path("data").path("components")) {
            JsonNode component = row.has("component") ? row.path("component") : row.path("components").path(0);
            if (id.equals(component.path("custom_id").asText())) return component;
        }
        throw new IllegalArgumentException("입력 항목이 없습니다: " + id);
    }

    private static String truncate(String value, int length) {
        if (value == null || value.isBlank()) return "-";
        return value.length() <= length ? value : value.substring(0, length - 1) + "…";
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://grafana.popn.gg";
        String stripped = value.strip();
        while (stripped.endsWith("/")) stripped = stripped.substring(0, stripped.length() - 1);
        return stripped;
    }

    private String performanceMessage(PerformanceDiagnostics.Snapshot snapshot, boolean diagnose) {
        String dashboard = grafanaUrl + "/d/popngg-production-overview/popn-gg-production-overview"
                + "?from=now-6h&to=now&timezone=browser&var-job=popngg-api&refresh=30s";
        if (!snapshot.available()) {
            return "**운영 지표 조회 실패**\n" + snapshot.error()
                    + " API 서비스에는 영향을 주지 않았습니다.\n[Grafana에서 직접 확인](" + dashboard + ")";
        }
        String status = diagnose ? diagnosticStatus(snapshot) : "현재 성능 요약";
        return "**" + status + "**\n"
                + "요청률: `" + number(snapshot.value("requestRate"), " req/s") + "`\n"
                + "평균 / P95 / P99: `" + number(snapshot.value("averageMs"), " ms") + " / "
                + number(snapshot.value("p95Ms"), " ms") + " / "
                + number(snapshot.value("p99Ms"), " ms") + "`\n"
                + "5xx: `" + percent(snapshot.value("errorRate")) + "`\n"
                + "API / 시스템 CPU: `" + percent(snapshot.value("apiCpu")) + " / "
                + percent(snapshot.value("systemCpu")) + "`\n"
                + "Hikari 대기 / JVM blocked: `" + number(snapshot.value("hikariPending"), "") + " / "
                + number(snapshot.value("blockedThreads"), "") + "`\n\n"
                + (diagnose ? "※ 읽기 전용 순간 지표 판정입니다. 장애 알림이나 Discord 스레드는 생성하지 않으며, 지속 시간은 Grafana에서 확인해야 합니다.\n" : "")
                + "[Grafana 상세 대시보드](" + dashboard + ")";
    }

    private static String diagnosticStatus(PerformanceDiagnostics.Snapshot snapshot) {
        if (atLeast(snapshot, "errorRate", 0.05) || atLeast(snapshot, "p95Ms", 5000)
                || atLeast(snapshot, "hikariPending", 1)) return "🔴 장애 의심";
        if (atLeast(snapshot, "p95Ms", 1000) || atLeast(snapshot, "apiCpu", 0.85)
                || atLeast(snapshot, "systemCpu", 0.85) || atLeast(snapshot, "blockedThreads", 1)) {
            return "🟡 성능 저하 의심";
        }
        return "🟢 현재 장애 징후 없음";
    }

    private static boolean atLeast(PerformanceDiagnostics.Snapshot snapshot, String name, double threshold) {
        Double value = snapshot.value(name);
        return value != null && value >= threshold;
    }

    private static String number(Double value, String unit) {
        return value == null ? "-" : "%.1f%s".formatted(value, unit);
    }

    private static String percent(Double value) {
        return value == null ? "-" : "%.1f%%".formatted(value * 100);
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
        return convertToPng(source);
    }

    static byte[] convertToPng(byte[] source) throws Exception {
        var image = ImageIO.read(new ByteArrayInputStream(source));
        if (image == null) throw new IllegalArgumentException("올바른 이미지가 아닙니다.");
        if ((long) image.getWidth() * image.getHeight() > 16_777_216L) throw new IllegalArgumentException("이미지 해상도가 너무 큽니다.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", output)) throw new IllegalArgumentException("PNG 변환에 실패했습니다.");
        byte[] png = output.toByteArray();
        if (png.length > 5 * 1024 * 1024) throw new IllegalArgumentException("변환된 PNG가 5MB를 초과합니다.");
        return png;
    }

    private record Prefill(String song, String genre, String artist, boolean upper) {}
    private record PreDraft(Prefill prefill, Instant requestedAt) {}
    private record Draft(CreateSongCommand command, String attachmentUrl, Instant createdAt) {}
    private record EditDraft(SongDetailView current, UpdateSongCommand command, String attachmentUrl,
                             Instant requestedCreatedAt, Instant requestedAt, Long reportId) {}
    @FunctionalInterface interface JacketDownloader { byte[] download(String url) throws Exception; }
}
