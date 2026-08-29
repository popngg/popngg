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

        when(unknown.findRecentUnresolved(20)).thenReturn(List.of(
                new UnknownChartReportPort.Report(7, "new song", "new genre", "artist",
                        4, false, 3, Instant.now())));
        assertThat(content(call(command("미등록목록")))).contains("new song", "3회");
    }

    @Test
    void opensCreateModalAndValidatesInputs() throws Exception {
        ObjectNode create = command("곡추가");
        option(create, "자켓", "file");
        option(create, "추가일", "2026-08-30");
        ObjectNode attachment = create.withObject("data").withObject("resolved")
                .withObject("attachments").putObject("file");
        attachment.put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/test.png");
        Map<?, ?> modal = body(call(create));
        assertThat(modal.get("type")).isEqualTo(9);
        String modalId = (String) ((Map<?, ?>) modal.get("data")).get("custom_id");

        ObjectNode submit = interaction(5);
        submit.withObject("data").put("custom_id", modalId);
        ArrayNode fields = submit.withObject("data").putArray("components");
        modalValue(fields, "song", "new song"); modalValue(fields, "genre", "genre");
        modalValue(fields, "artist", "artist"); modalValue(fields, "version", "29");
        modalValue(fields, "charts", "N:30,H:42,EX:48");
        String confirmId = firstButtonId(body(call(submit)));
        when(jackets.uploadPng(anyString(), any())).thenReturn("https://static.popn.gg/hash.png");
        when(createSong.execute(any())).thenReturn(new CreateSongResult(99, List.of(1L, 2L, 3L)));
        ObjectNode confirm = interaction(3);
        confirm.withObject("data").put("custom_id", confirmId);
        assertThat(content(call(confirm))).contains("곡 등록 완료", "99");
        verify(admin).send(contains("곡 추가"));

        option(create, "추가일", "bad-date");
        assertThat(content(call(create))).contains("YYYY-MM-DD");
    }

    @Test
    void editsSongThroughPreviewAndConfirmation() throws Exception {
        SongDetailView before = detail("old", "old-hash");
        when(findDetail.findSong(12)).thenReturn(before);
        ObjectNode edit = command("곡수정");
        option(edit, "song_id", 12);
        option(edit, "자켓", "edit-file");
        edit.withObject("data").withObject("resolved").withObject("attachments")
                .putObject("edit-file").put("size", 100).put("content_type", "image/png")
                .put("url", "https://cdn.discordapp.com/edit.png");
        Map<?, ?> modal = body(call(edit));
        String modalId = (String) ((Map<?, ?>) modal.get("data")).get("custom_id");

        ObjectNode submit = interaction(5);
        submit.withObject("data").put("custom_id", modalId);
        ArrayNode components = submit.withObject("data").putArray("components");
        modalValue(components, "song", "changed");
        modalValue(components, "genre", "genre");
        modalValue(components, "artist", "artist");
        modalValue(components, "version", "29");
        modalValue(components, "charts", "N:30,H:42");
        Map<?, ?> preview = body(call(submit));
        String confirmId = firstButtonId(preview);

        when(updateSong.execute(any())).thenReturn(detail("changed", "new-hash"));
        when(jackets.uploadPng(anyString(), any())).thenReturn("https://static.popn.gg/new-hash.png");
        ObjectNode confirm = interaction(3);
        confirm.withObject("data").put("custom_id", confirmId);
        assertThat(content(call(confirm))).contains("곡 수정 완료", "new-hash");
        verify(updateSong).execute(any());
        verify(admin).send(contains("곡 수정"));
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
