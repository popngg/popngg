package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.out.OfficialSongSource;
import gg.popn.application.song.port.out.ReviewedSongRegistrationPort;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OfficialSongReviewTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OfficialSongSource source = mock(OfficialSongSource.class);
    private final ReviewedSongRegistrationPort registration = mock(ReviewedSongRegistrationPort.class);
    private final List<Map<String,Object>> replies = new ArrayList<>();
    private final OfficialSongReview review = new OfficialSongReview(source, registration, Runnable::run, (root, data) -> replies.add(data));
    private final OfficialSongSource.Song song = new OfficialSongSource.Song("Song", "Genre", "Artist", 29, false,
            List.of(new OfficialSongSource.Chart(2,25), new OfficialSongSource.Chart(4,49)),
            "https://p.eagate.573.jp/game/popn/popn29/music/list.html?version=29");
    private final UnknownChartReportPort.Report report = new UnknownChartReportPort.Report(7, "Song", "old", "Artist", null, false, false, 1, Instant.now());

    @Test void confirmationButtonRegistersImmediatelyWithVisibleDefaultsAndNoModal() {
        String id = start();
        assertThat(replies.getLast().toString()).contains("기본값", "공식 제공 정보 아님", "특수 게이지·판정: 없음", "정보 수정");
        when(registration.register(eq(7L), any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(1, gg.popn.application.song.dto.command.CreateSongCommand.class);
            assertThat(command.jacketUrl()).isNull();
            assertThat(command.createdAt()).isNull();
            assertThat(command.charts()).allSatisfy(chart -> {
                assertThat(chart.chartVersion()).isEqualTo(29);
                assertThat(chart.hasStrictGauge()).isFalse();
                assertThat(chart.hasStrictJudgement()).isFalse();
            });
            return new CreateSongResult(55,List.of(1L,2L));
        });
        var confirm = request(3,"official_confirm:"+id);
        assertThat(review.interact(confirm).get("type")).isEqualTo(5);
        assertThat(replies.getLast().toString()).contains("등록 완료");
        review.interact(confirm);
        verify(registration,times(1)).register(eq(7L),any());
    }

    @Test void directConfirmationRejectsAnotherAdministratorAndCancelledDraft() {
        String id = start();
        var other = request(3,"official_confirm:"+id);
        other.withObject("member").withObject("user").put("id","another");
        assertThat(review.interact(other).toString()).contains("본인의 요청");
        review.interact(request(3,"official_cancel:"+id));
        review.interact(request(3,"official_confirm:"+id));
        verify(registration,never()).register(anyLong(),any());
    }

    @Test void busyQueueKeepsConfirmationDraftAvailableForRetry() {
        var accept = new java.util.concurrent.atomic.AtomicBoolean(true);
        var retryable = new OfficialSongReview(source, registration, task -> {
            if (!accept.get()) throw new RejectedExecutionException();
            task.run();
        }, (root,data) -> replies.add(data));
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of(song));
        retryable.start(request(3,"select"),report);
        String id=mapper.valueToTree(replies.getLast()).path("components").get(0).path("components").get(0)
                .path("custom_id").asText().split(":")[1];
        accept.set(false);
        assertThat(retryable.interact(request(3,"official_confirm:"+id)).toString()).contains("요청이 많습니다");
        verify(registration,never()).register(anyLong(),any());
        accept.set(true);
        when(registration.register(eq(7L),any())).thenReturn(new CreateSongResult(55,List.of(1L,2L)));
        assertThat(retryable.interact(request(3,"official_confirm:"+id)).get("type")).isEqualTo(5);
        verify(registration).register(eq(7L),any());
    }

    @Test void previewsWithoutWritesAndRegistersOnceOnlyAfterOwnerSubmitsConfirmation() {
        String id = start();
        verify(registration, never()).register(anyLong(), any());
        assertThat(review.interact(request(3,"official_review:"+id)).get("type")).isEqualTo(9);
        verify(registration, never()).register(anyLong(), any());
        when(registration.register(eq(7L), any())).thenAnswer(invocation -> {
            var command = invocation.getArgument(1, gg.popn.application.song.dto.command.CreateSongCommand.class);
            assertThat(command.jacketUrl()).isNull();
            assertThat(command.createdAt()).isNull();
            assertThat(command.charts()).hasSize(2);
            assertThat(command.charts().get(1).hasStrictGauge()).isTrue();
            assertThat(command.charts().get(1).chartVersion()).isEqualTo(29);
            return new CreateSongResult(55, List.of(1L,2L));
        });
        var submit = submission(id);
        assertThat(review.interact(submit).get("type")).isEqualTo(5);
        assertThat(replies.getLast().get("content").toString()).contains("등록 완료", "자켓 없음");
        review.interact(submit);
        verify(registration, times(1)).register(eq(7L),any());
    }

    @Test void refusesOtherOwnersAndCancellationPreventsWrites() {
        String id=start();
        var stranger=request(3,"official_review:"+id);
        stranger.withObject("member").withObject("user").put("id","stranger");
        assertThat(review.interact(stranger).toString()).contains("본인의 요청");
        assertThat(review.interact(request(3,"official_cancel:"+id)).toString()).contains("취소");
        review.interact(submission(id));
        verify(registration,never()).register(anyLong(),any());
    }

    @Test void ambiguousMissingExistingAndFailedLookupsDoNotCreateDrafts() {
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of(song,song));
        review.start(request(3,"select"),report);
        assertThat(replies.getLast().toString()).contains("후보가 여러 개");
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of());
        review.start(request(3,"select"),report);
        assertThat(replies.getLast().toString()).contains("공식 정보가 없거나");
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of(song));
        when(registration.findExisting(anyString(),anyString(),anyBoolean())).thenReturn(List.of(55L));
        review.start(request(3,"select"),report);
        assertThat(replies.getLast().toString()).contains("이미 등록", "곡수정");
        when(source.find(anyString(),anyString(),any())).thenThrow(new IllegalStateException());
        review.start(request(3,"select"),report);
        assertThat(replies.getLast().toString()).contains("완료하지 못했습니다");
        verify(registration,never()).register(anyLong(),any());
    }

    @Test void requiresExplicitFlagsAndCompleteChartVersions() {
        String id=start();
        var invalid = submission(id);
        ((ObjectNode) invalid.path("data").path("components").get(1).path("components").get(0)).put("value", "");
        assertThat(review.interact(invalid).toString()).contains("입력 오류");
        verify(registration,never()).register(anyLong(),any());
        var missing=submission(id);
        ((ObjectNode) missing.path("data").path("components").get(0).path("components").get(0)).put("value", "N:29");
        assertThatThrownBy(() -> OfficialSongReview.command(song,missing)).isInstanceOf(IllegalArgumentException.class);
        var foreign=submission(id);
        ((ObjectNode) foreign.path("data").path("components").get(1).path("components").get(0)).put("value", "H");
        assertThatThrownBy(() -> OfficialSongReview.command(song,foreign)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void boundedQueueAndReplyFailuresNeverTriggerRegistrationDuringLookup() {
        var busy=new OfficialSongReview(source,registration,r->{throw new RejectedExecutionException();},(r,d)->{});
        assertThat(busy.start(request(3,"select"),report).toString()).contains("조회 요청이 많습니다");
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of(song));
        var disconnected=new OfficialSongReview(source,registration,Runnable::run,(r,d)->{throw new IllegalStateException();});
        assertThat(disconnected.start(request(3,"select"),report).get("type")).isEqualTo(5);
        verify(registration,never()).register(anyLong(),any());
    }

    private String start() {
        when(source.find(anyString(),anyString(),any())).thenReturn(List.of(song));
        assertThat(review.start(request(3,"select"),report).get("type")).isEqualTo(5);
        var json=mapper.valueToTree(replies.getLast());
        return json.path("components").get(0).path("components").get(0).path("custom_id").asText().split(":")[1];
    }
    private ObjectNode request(int type,String id) {
        var root=mapper.createObjectNode().put("type",type).put("guild_id","guild");
        root.withObject("member").withObject("user").put("id","admin");
        root.withObject("data").put("custom_id",id);
        return root;
    }
    private ObjectNode submission(String id) {
        var root=request(5,"official_submit:"+id);
        var rows=root.withObject("data").withArray("components");
        for (var entry:Map.of("versions","N:29,EX:29","gauge","EX","judgement","없음","date","").entrySet().stream()
                .sorted(Comparator.comparingInt(e->List.of("versions","gauge","judgement","date").indexOf(e.getKey()))).toList()) {
            rows.addObject().put("type",1).withArray("components").addObject().put("custom_id",entry.getKey()).put("value",entry.getValue());
        }
        return root;
    }
}
