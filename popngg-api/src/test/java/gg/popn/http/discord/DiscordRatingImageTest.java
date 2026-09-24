package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.analysis.RatingSnapshot;
import gg.popn.application.playdata.dto.result.PlaydataQueryResults;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.http.analysis.RatingController;
import gg.popn.http.analysis.TierListImageRenderer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscordRatingImageTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RatingQuery ratings = mock(RatingQuery.class);
    private final PlaydataQueryUseCase playdata = mock(PlaydataQueryUseCase.class);
    private final TierListImageRenderer renderer = mock(TierListImageRenderer.class);
    private final List<DiscordRatingImage.Result> replies = new ArrayList<>();
    private final DiscordRatingImage image = new DiscordRatingImage(
            ratings, playdata, renderer, Runnable::run, (root, result) -> replies.add(result));

    @Test
    void rendersTheSelectedUsersScoreAndMedalTierList() throws Exception {
        var snapshot = new RatingSnapshot("snapshot", "now", "v1", "EXPERIMENTAL",
                "NOT_VALIDATED", 50, List.of());
        var user = new PlaydataQueryResults.UserPlaydata(
                "1234-5678-9012", "테스트 유저", 0, 0, 0, List.of());
        when(ratings.latest()).thenReturn(snapshot);
        when(playdata.findUserPlaydata("1234-5678-9012")).thenReturn(user);
        when(renderer.render(snapshot, user, 49, RatingController.Metric.SPI))
                .thenReturn(new byte[]{1, 2, 3});

        Map<String, Object> response = image.start(root(), 49, "spi", " 1234-5678-9012 ");

        assertThat(response.get("type")).isEqualTo(5);
        assertThat(((Map<?, ?>) response.get("data")).containsKey("flags")).isFalse();
        assertThat(replies).singleElement().satisfies(result -> {
            assertThat(result.content()).contains("테스트 유저", "Lv49 SPI", "최고 기록", "실험 단계");
            assertThat(result.filename()).isEqualTo("popngg-lv49-spi-1234-5678-9012.png");
            assertThat(result.png()).containsExactly(1, 2, 3);
            assertThat(result.hasImage()).isTrue();
        });
    }

    @Test
    void validatesInputsBeforeStartingImageWork() {
        assertThat(image.start(root(), 47, "CPI", "1234-5678-9012").toString()).contains("48, 49, 50");
        assertThat(image.start(root(), 49, "CPI", "BOT-1-1").toString()).contains("1234-5678-9012");
        assertThat(image.start(root(), 49, "OTHER", "1234-5678-9012").toString()).contains("CPI 또는 SPI");
        verifyNoInteractions(ratings, playdata, renderer);
    }

    @Test
    void reportsKnownGenerationFailuresToTheDeferredReply() throws Exception {
        when(ratings.latest()).thenThrow(new IllegalStateException());
        image.start(root(), 48, "CPI", "1234-5678-9012");
        assertThat(replies.getLast().content()).contains("아직 준비되지 않았습니다");
        assertThat(replies.getLast().hasImage()).isFalse();

        reset(ratings);
        when(ratings.latest()).thenReturn(mock(RatingSnapshot.class));
        when(playdata.findUserPlaydata(anyString())).thenThrow(new IllegalArgumentException());
        image.start(root(), 48, "CPI", "1234-5678-9012");
        assertThat(replies.getLast().content()).contains("사용자를 찾을 수 없습니다");

        reset(playdata);
        when(renderer.render(any(), any(), anyInt(), any())).thenThrow(new RuntimeException());
        image.start(root(), 48, "CPI", "1234-5678-9012");
        assertThat(replies.getLast().content()).contains("생성하지 못했습니다");
    }

    @Test
    void rejectsWorkWhenTheBoundedQueueIsBusy() {
        var busy = new DiscordRatingImage(ratings, playdata, renderer,
                task -> { throw new RejectedExecutionException(); }, (root, result) -> {});
        assertThat(busy.start(root(), 50, "CPI", "1234-5678-9012").toString())
                .contains("요청이 많습니다");
    }

    private ObjectNode root() {
        return mapper.createObjectNode().put("application_id", "123").put("token", "token");
    }
}
