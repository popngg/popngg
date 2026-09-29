package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstants;
import gg.popn.http.analysis.AchievementConstantImageRenderer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiscordAchievementConstantImageTest {
    private final AchievementConstants constants=mock(AchievementConstants.class);
    private final AchievementConstantImageRenderer renderer=mock(AchievementConstantImageRenderer.class);
    private final List<DiscordAchievementConstantImage.Result> replies=new ArrayList<>();
    private final DiscordAchievementConstantImage image=new DiscordAchievementConstantImage(
            constants,renderer,Runnable::run,(root,result)->replies.add(result));

    @Test void rendersAdminConstantTable()throws Exception{
        var snapshot=new AchievementConstants.Snapshot(1,"source","achievement-v1","EXPERIMENTAL",
                Instant.parse("2026-09-25T00:00:00Z"),AchievementConstants.Axis.MEDAL,49,List.of());
        when(constants.latest(49,AchievementConstants.Axis.MEDAL)).thenReturn(snapshot);
        when(renderer.render(snapshot)).thenReturn(new byte[]{1,2,3});
        Map<String,Object> response=image.start(new ObjectMapper().createObjectNode(),49,"medal");
        assertThat(response.get("type")).isEqualTo(5);
        assertThat(((Map<?,?>)response.get("data")).get("flags")).isEqualTo(64);
        assertThat(replies).singleElement().satisfies(result->{
            assertThat(result.content()).contains("Lv49","메달","관리자 검토용");
            assertThat(result.filename()).isEqualTo("popngg-lv49-medal-constants.png");
        });
    }

    @Test void validatesAndReportsNotReady(){
        assertThat(image.start(new ObjectMapper().createObjectNode(),47,"MEDAL").toString()).contains("48, 49, 50");
        assertThat(image.start(new ObjectMapper().createObjectNode(),49,"CPI").toString()).contains("메달 또는 랭크");
        when(constants.latest(49,AchievementConstants.Axis.RANK)).thenThrow(new IllegalStateException());
        image.start(new ObjectMapper().createObjectNode(),49,"RANK");
        assertThat(replies.getLast().content()).contains("아직 저장되지 않았습니다");
    }

    @Test void rejectsBusyQueue(){
        var busy=new DiscordAchievementConstantImage(constants,renderer,
                task->{throw new RejectedExecutionException();},(root,result)->{});
        assertThat(busy.start(new ObjectMapper().createObjectNode(),49,"MEDAL").toString()).contains("요청이 많습니다");
    }
}
