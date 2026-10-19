package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.tier.*;
import org.junit.jupiter.api.Test;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Flow;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class DiscordAchievementCommandTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private com.fasterxml.jackson.databind.JsonNode request(int level, int page) throws Exception {
        return mapper.readTree("""
                {"application_id":"123","token":"test-token","data":{"options":[
                {"name":"레벨","value":%d},{"name":"기준","value":"S"},{"name":"페이지","value":%d}]}}
                """.formatted(level, page));
    }
    @Test void defersAndEditsOriginalWithMultipartPng() throws Exception {
        var service = mock(AchievementBoardService.class);
        var renderer = mock(AchievementBoardImagePort.class);
        var client = mock(HttpClient.class);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        var board = new AchievementBoard(40, AchievementBoard.Metric.S, Instant.now(), List.of());
        when(service.load(40, AchievementBoard.Metric.S)).thenReturn(board);
        when(renderer.render(board, 1)).thenReturn(new byte[]{1,2,3});
        var command = new DiscordAchievementCommand(service, renderer, mapper, client);
        try {
            assertThat(command.execute(request(40, 1)).get("type")).isEqualTo(5);
            var capture = ArgumentCaptor.forClass(HttpRequest.class);
            verify(client, timeout(3000)).send(capture.capture(), any(HttpResponse.BodyHandler.class));
            assertThat(capture.getValue().method()).isEqualTo("PATCH");
            assertThat(capture.getValue().uri().getPath()).endsWith("/messages/@original");
            var body = new ByteArrayOutputStream();
            capture.getValue().bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
                public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
                public void onNext(ByteBuffer b) { byte[] bytes = new byte[b.remaining()]; b.get(bytes); body.writeBytes(bytes); }
                public void onError(Throwable t) { throw new AssertionError(t); }
                public void onComplete() {}
            });
            assertThat(body.toString(java.nio.charset.StandardCharsets.UTF_8)).contains("image/png", "files[0]", "popngg-lv40-s-1.png", "allowed_mentions");
        } finally { command.close(); }
    }
    @Test void rejectsInvalidInputBeforeDispatch() throws Exception {
        var service = mock(AchievementBoardService.class);
        var command = new DiscordAchievementCommand(service, mock(AchievementBoardImagePort.class), mapper, mock(HttpClient.class));
        try {
            assertThat(command.execute(request(39, 1)).get("type")).isEqualTo(4);
            assertThat(command.execute(request(50, 0)).get("type")).isEqualTo(4);
            verifyNoInteractions(service);
            assertThat(DiscordCommandRegistrar.achievementOptions()).hasSize(3);
        } finally { command.close(); }
    }

    @Test void reportsOutOfRangePageWithoutRendering() throws Exception {
        var service = mock(AchievementBoardService.class);
        var renderer = mock(AchievementBoardImagePort.class);
        var client = mock(HttpClient.class);
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        when(service.load(40, AchievementBoard.Metric.S)).thenReturn(
                new AchievementBoard(40, AchievementBoard.Metric.S, Instant.now(), List.of()));
        var command = new DiscordAchievementCommand(service, renderer, mapper, client);
        try {
            assertThat(command.execute(request(40, 2)).get("type")).isEqualTo(5);
            verify(client, timeout(3000)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
            verifyNoInteractions(renderer);
        } finally { command.close(); }
    }
}
