package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import gg.popn.application.song.dto.result.*;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.in.*;
import gg.popn.application.song.port.out.AdminNotificationPort;
import gg.popn.application.song.port.out.JacketStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiscordInteractionControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CreateSongUseCase createSong = mock(CreateSongUseCase.class);
    private final FindSongsUseCase findSongs = mock(FindSongsUseCase.class);
    private final JacketStoragePort jackets = mock(JacketStoragePort.class);
    private final FindSongDetailUseCase findDetail = mock(FindSongDetailUseCase.class);
    private final UpdateSongUseCase updateSong = mock(UpdateSongUseCase.class);
    private final AdminNotificationPort admin = mock(AdminNotificationPort.class);
    private final UnknownChartReportPort unknown = mock(UnknownChartReportPort.class);
    private KeyPair keys;
    private DiscordInteractionController controller;

    @BeforeEach
    void setUp() throws Exception {
        keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] encoded = keys.getPublic().getEncoded();
        String rawPublicKey = HexFormat.of().formatHex(
                java.util.Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length));
        controller = new DiscordInteractionController(mapper, createSong, findSongs, jackets,
                findDetail, updateSong, admin, unknown, rawPublicKey, "guild", "admin", url -> new byte[]{1});
    }

    @Test
    void validatesPingSignatureAndRejectsInvalidSignature() throws Exception {
        ObjectNode ping = mapper.createObjectNode().put("type", 1);
        assertThat(call(ping).getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) call(ping).getBody()).get("type")).isEqualTo(1);
        assertThat(controller.interact(mapper.writeValueAsBytes(ping), "00", epoch())
                .getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void requiresConfiguredGuildAndRole() throws Exception {
        ObjectNode command = command("곡조회");
        command.withObject("member").withArray("roles").removeAll();
        assertThat(content(call(command))).contains("관리자 역할");
    }

    @Test
    void searchesSongsAndListsUnknownCharts() throws Exception {
        var song = new GroupedSongView(3, "hash", "genre", "title", "artist", 29, null, List.of());
        when(findSongs.execute(any())).thenReturn(SongPageView.of(List.of(song), 0, 10, 1));
        ObjectNode search = command("곡조회");
        option(search, "검색어", "title");
        assertThat(content(call(search))).contains("title", "#3");
        assertThat(((Map<?, ?>) body(call(search)).get("data")).containsKey("flags")).isFalse();

        when(unknown.findRecentUnresolved(anyInt())).thenReturn(List.of(
                new UnknownChartReportPort.Report(7, "new song", "new genre", "artist",
                        3, Instant.now())));
        assertThat(content(call(command("미등록목록")))).contains("new song", "3회")
                .doesNotContain("난이도", "UPPER");

        ObjectNode selection = interaction(3);
        selection.withObject("data").put("custom_id", "unknown_song_select")
                .putArray("values").add("7");
        Map<?, ?> modal = body(call(selection));
        Map<?, ?> modalData = (Map<?, ?>) modal.get("data");
        assertThat((List<?>) modalData.get("components")).hasSize(5);
        assertThat(modalData.toString()).contains("new song", "new genre", "artist");

        ObjectNode submit = interaction(5);
        submit.withObject("data").put("custom_id", modalData.get("custom_id").toString());
        ArrayNode fields = submit.withObject("data").putArray("components");
        modalUpload(fields, "jacket", "unknown-file");
        modalModernValue(fields, "date", "2026-08-30");
        modalModernValue(fields, "metadata", "{\"songName\":\"new song\",\"genreName\":\"new genre\",\"artistName\":\"artist\",\"upper\":false}");
        modalModernValue(fields, "version", "29");
        modalModernValue(fields, "levels", "L:[], N:[30], H:[42], EX:[48]");
        submit.withObject("data").withObject("resolved").withObject("attachments")
                .putObject("unknown-file").put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/unknown.png");
        assertThat(content(call(submit))).contains("곡 등록 JSON", "new song");
    }

    @Test
    void acceptsNonSquareJacketImages() throws Exception {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream source = new ByteArrayOutputStream();
        ImageIO.write(image, "png", source);

        byte[] converted = DiscordInteractionController.convertToPng(source.toByteArray());

        BufferedImage result = ImageIO.read(new java.io.ByteArrayInputStream(converted));
        assertThat(result.getWidth()).isEqualTo(2);
        assertThat(result.getHeight()).isEqualTo(1);
    }

    @Test
    void previewsAndCreatesSongFromSingleCommand() throws Exception {
        ObjectNode create = command("곡추가");
        option(create, "자켓", "file"); option(create, "추가일", "2026-08-30");
        option(create, "곡명", "new song"); option(create, "장르", "genre");
        option(create, "아티스트", "artist"); option(create, "버전", 29);
        option(create, "upper", "x"); option(create, "n", 30);
        option(create, "h", 42); option(create, "ex", 48);
        create.withObject("data").withObject("resolved").withObject("attachments").putObject("file")
                .put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/test.png");
        Map<?, ?> preview = body(call(create));
        assertThat((String) ((Map<?, ?>) preview.get("data")).get("content"))
                .contains("```json", "new song", "\"N\" : 30");
        String confirmId = firstButtonId(preview);
        when(jackets.uploadPng(anyString(), any())).thenReturn("https://static.popn.gg/hash.png");
        when(createSong.execute(any())).thenReturn(new CreateSongResult(99, List.of(1L, 2L, 3L)));
        ObjectNode confirm = interaction(3);
        confirm.withObject("data").put("custom_id", confirmId);
        assertThat(content(call(confirm))).contains("곡 등록 완료", "99");
        verify(admin).send(contains("곡 추가"));
    }

    @Test
    void editsSongThroughPreviewAndConfirmation() throws Exception {
        SongDetailView before = detail("old", "old-hash");
        when(findDetail.findSong(12)).thenReturn(before);
        ObjectNode edit = command("곡수정");
        option(edit, "song_id", 12);
        option(edit, "자켓", "edit-file");
        option(edit, "곡명", "changed"); option(edit, "장르", "genre");
        option(edit, "아티스트", "artist"); option(edit, "버전", 29);
        option(edit, "upper", "x"); option(edit, "n", 30); option(edit, "h", 42);
        edit.withObject("data").withObject("resolved").withObject("attachments")
                .putObject("edit-file").put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/edit.png");
        Map<?, ?> preview = body(call(edit));
        assertThat((String) ((Map<?, ?>) preview.get("data")).get("content"))
                .contains("```json", "changed");
        String confirmId = firstButtonId(preview);

        when(updateSong.execute(any())).thenReturn(detail("changed", "new-hash"));
        when(jackets.uploadPng(anyString(), any())).thenReturn("https://static.popn.gg/new-hash.png");
        ObjectNode confirm = interaction(3);
        confirm.withObject("data").put("custom_id", confirmId);
        assertThat(content(call(confirm))).contains("곡 수정 완료", "new-hash");
        verify(updateSong).execute(any());
        verify(admin).send(contains("곡 수정"));
    }

    @Test
    void reportsExpiredUnknownSongSelection() throws Exception {
        ObjectNode selection = interaction(3);
        selection.withObject("data").put("custom_id", "unknown_song_select")
                .putArray("values").add("999");
        assertThat(content(call(selection))).contains("찾을 수 없습니다");
    }

    @Test
    void opensExistingSongEditorForIncompleteMetadataAndResolvesAfterUpdate() throws Exception {
        when(unknown.findRecentIncomplete(anyInt())).thenReturn(List.of(
                new UnknownChartReportPort.IncompleteReport(8, 12, "old", "genre",
                        "reported artist", "", 2, Instant.now())));
        assertThat(content(call(command("정보보완목록"))))
                .contains("정보 보완", "reported artist", "old");

        when(findDetail.findSong(12)).thenReturn(detail("old", "old-hash"));
        ObjectNode selection = interaction(3);
        selection.withObject("data").put("custom_id", "incomplete_song_select")
                .putArray("values").add("8");
        Map<?, ?> modal = body(call(selection));
        Map<?, ?> modalData = (Map<?, ?>) modal.get("data");
        assertThat(modalData.toString()).contains("곡 수정", "old", "genre");

        ObjectNode submit = interaction(5);
        submit.withObject("data").put("custom_id", modalData.get("custom_id").toString());
        ArrayNode fields = submit.withObject("data").putArray("components");
        modalValue(fields, "song", "old"); modalValue(fields, "genre", "genre");
        modalValue(fields, "artist", "reported artist"); modalValue(fields, "version", "29");
        modalValue(fields, "charts", "N:30,H:42,EX:48");
        Map<?, ?> preview = body(call(submit));
        String confirmId = firstButtonId(preview);

        when(updateSong.execute(any())).thenReturn(detail("old", "new-hash"));
        ObjectNode confirm = interaction(3);
        confirm.withObject("data").put("custom_id", confirmId);
        assertThat(content(call(confirm))).contains("곡 수정 완료");
        verify(unknown).resolve(8);
    }

    @Test
    void preservesOmittedSongUpdateValuesAndRejectsCreationWithoutCharts() throws Exception {
        SongDetailView before = detail("old", "old-hash");
        when(findDetail.findSong(12)).thenReturn(before);

        ObjectNode edit = command("곡수정");
        option(edit, "song_id", 12);
        option(edit, "곡명", "metadata only");
        Map<?, ?> preview = body(call(edit));
        assertThat((String) ((Map<?, ?>) preview.get("data")).get("content"))
                .contains("metadata only", "\"version\" : 29", "\"charts\" : [ ]");

        ObjectNode create = command("곡추가");
        option(create, "자켓", "file"); option(create, "추가일", "2026-08-30");
        option(create, "곡명", "new"); option(create, "장르", "genre");
        option(create, "아티스트", "artist"); option(create, "버전", 29);
        option(create, "upper", "x");
        create.withObject("data").withObject("resolved").withObject("attachments").putObject("file")
                .put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/test.png");
        assertThat(content(call(create))).contains("하나 이상");
    }

    private SongDetailView detail(String title, String hash) {
        var metadata = new SongMetadataView(12, hash, "genre", title, "artist", 29,
                "https://static.popn.gg/" + hash + ".png");
        return new SongDetailView(metadata, List.of(
                new ChartMetadataView(1, new DifficultyView(2, "NORMAL", "N", 2),
                        30, 29, false, false, false, false),
                new ChartMetadataView(2, new DifficultyView(3, "HYPER", "H", 3),
                        42, 29, false, false, false, false)));
    }

    private ObjectNode command(String name) {
        ObjectNode root = interaction(2);
        root.withObject("data").put("name", name).putArray("options");
        return root;
    }

    private ObjectNode interaction(int type) {
        ObjectNode root = mapper.createObjectNode().put("type", type).put("guild_id", "guild");
        root.withObject("member").putObject("user").put("id", "user");
        root.withObject("member").putArray("roles").add("admin");
        return root;
    }

    private void option(ObjectNode root, String name, Object value) {
        ArrayNode options = root.withObject("data").withArray("options");
        for (JsonNode node : options) if (name.equals(node.path("name").asText())) {
            ((ObjectNode) node).set("value", mapper.valueToTree(value)); return;
        }
        ObjectNode option = options.addObject().put("name", name);
        option.set("value", mapper.valueToTree(value));
    }

    private void modalValue(ArrayNode rows, String id, String value) {
        rows.addObject().putArray("components").addObject()
                .put("custom_id", id).put("value", value);
    }

    private void modalModernValue(ArrayNode rows, String id, String value) {
        rows.addObject().put("type", 18).putObject("component")
                .put("type", 4).put("custom_id", id).put("value", value);
    }

    private void modalUpload(ArrayNode rows, String id, String attachmentId) {
        rows.addObject().put("type", 18).putObject("component")
                .put("type", 19).put("custom_id", id).putArray("values").add(attachmentId);
    }

    private org.springframework.http.ResponseEntity<?> call(ObjectNode payload) throws Exception {
        byte[] body = mapper.writeValueAsBytes(payload);
        String timestamp = epoch();
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate());
        signer.update(timestamp.getBytes(StandardCharsets.UTF_8));
        signer.update(body);
        return controller.interact(body, HexFormat.of().formatHex(signer.sign()), timestamp);
    }

    private String epoch() { return Long.toString(Instant.now().getEpochSecond()); }
    @SuppressWarnings("unchecked") private Map<?, ?> body(org.springframework.http.ResponseEntity<?> r) { return (Map<?, ?>) r.getBody(); }
    private String content(org.springframework.http.ResponseEntity<?> r) {
        Map<?, ?> data = (Map<?, ?>) body(r).get("data"); return (String) data.get("content");
    }
    private String firstButtonId(Map<?, ?> response) {
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        List<?> rows = (List<?>) data.get("components");
        List<?> buttons = (List<?>) ((Map<?, ?>) rows.getFirst()).get("components");
        return (String) ((Map<?, ?>) buttons.getFirst()).get("custom_id");
    }
}
