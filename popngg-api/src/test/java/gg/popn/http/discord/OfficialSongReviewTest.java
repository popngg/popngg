package gg.popn.http.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gg.popn.application.playdata.port.out.UnknownChartReportPort;
import gg.popn.application.song.dto.command.CreateSongCommand;
import gg.popn.application.song.dto.result.CreateSongResult;
import gg.popn.application.song.port.out.ReviewedSongRegistrationPort;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OfficialSongReviewTest {
    final ObjectMapper mapper = new ObjectMapper();
    final ReviewedSongRegistrationPort registration = mock(ReviewedSongRegistrationPort.class);
    final List<Map<String,Object>> replies = new ArrayList<>();
    final OfficialSongReview review = new OfficialSongReview(registration, Runnable::run, (r,d) -> replies.add(d));
    final UnknownChartReportPort.Report report = new UnknownChartReportPort.Report(7,"Any%","Genre","Artist",null,false,false,1,Instant.now());

    @Test void selectionImmediatelyOpensEmptyRequiredLevelAndVersionInputsWithoutDatabaseAccess() {
        var response = review.start(request(3,"select"),report);
        assertThat(response.get("type")).isEqualTo(9);
        var modal = mapper.valueToTree(response).path("data");
        assertThat(modal.path("components")).hasSize(5);
        assertThat(modal.path("components").get(0).path("components").get(0).path("value").asText()).isEmpty();
        assertThat(modal.path("components").get(0).path("components").get(0).path("required").asBoolean()).isTrue();
        assertThat(modal.toString()).doesNotContain("확인·등록");
        verifyNoInteractions(registration);
    }

    @Test void enteredLevelsArePreviewedThenRegisteredOnlyAfterExplicitConfirmationOnceWithoutJacket() {
        String id = start(review);
        assertThat(review.interact(request(3,"official_confirm:"+id)).toString()).contains("먼저 입력");
        var preview = review.interact(submit(id,"N:25,H:38,EX:45"));
        assertThat(preview.toString()).contains("관리자가 입력", "N:25", "H:38", "EX:45", "확인·등록", "Any%");
        verifyNoInteractions(registration);
        when(registration.register(eq(7L),any())).thenAnswer(invocation -> {
            var command=invocation.getArgument(1,CreateSongCommand.class);
            assertThat(command.jacketUrl()).isNull();
            assertThat(command.charts()).extracting(CreateSongCommand.CreateChartCommand::level).containsExactly(25,38,45);
            assertThat(command.charts()).allSatisfy(c -> assertThat(c.chartVersion()).isEqualTo(29));
            return new CreateSongResult(55,List.of(1L,2L,3L));
        });
        review.interact(request(3,"official_confirm:"+id));
        assertThat(replies.getLast().toString()).contains("등록 완료");
        review.interact(request(3,"official_confirm:"+id));
        verify(registration,times(1)).register(eq(7L),any());
    }

    @Test void invalidLevelsCanBeReenteredAndEditPreservesEnteredLevels() {
        String id = start(review);
        for(String levels:List.of("", "EX:0", "EX:51", "EX:45,EX:46", "UNKNOWN:20", "EX:abc", "N:25,")) {
            assertThat(review.interact(submit(id,levels)).toString()).contains("입력 오류", "다시 입력");
        }
        assertThat(review.interact(submit(id,"EX:45")).toString()).contains("EX:45");
        var modal=review.interact(request(3,"official_review:"+id));
        assertThat(modal.get("type")).isEqualTo(9);
        assertThat(modal.toString()).contains("EX:45");
        verifyNoInteractions(registration);
    }

    @Test void rejectsInvalidVersionVariantAndFlagsAndSupportsUpper() {
        String id=start(review);
        for(var entry:Map.of("version","100","upper","unknown","gauge","H").entrySet()) {
            var input=submit(id,"EX:45");
            change(input,entry.getKey(),entry.getValue());
            assertThat(review.interact(input).toString()).contains("입력 오류");
        }
        var input=submit(id,"EX:45"); change(input,"upper","UPPER"); change(input,"gauge","EX");
        assertThat(review.interact(input).toString()).contains("29 / true", "특수 게이지", "EX");
        verifyNoInteractions(registration);
    }

    @Test void otherOwnersCancellationAndMissingDraftCannotRegister() {
        String id=start(review);
        var stranger=submit(id,"EX:45"); stranger.withObject("member").withObject("user").put("id","other");
        assertThat(review.interact(stranger).toString()).contains("본인의 요청");
        review.interact(request(3,"official_cancel:"+id));
        assertThat(review.interact(submit(id,"EX:45")).toString()).contains("만료");
        verifyNoInteractions(registration);
    }

    @Test void failedWebhookCanRecoverRegistrationResultWithoutRepeatingWrites() {
        List<Runnable> tasks=new ArrayList<>();
        var disconnected=new OfficialSongReview(registration,tasks::add,(r,d)->{throw new IllegalStateException();});
        String id=start(disconnected); disconnected.interact(submit(id,"EX:45"));
        when(registration.register(eq(7L),any())).thenReturn(new CreateSongResult(55,List.of(1L)));
        var pending=disconnected.interact(request(3,"official_confirm:"+id));
        String status=mapper.valueToTree(pending).path("data").path("components").get(0).path("components").get(0).path("custom_id").asText();
        assertThat(disconnected.interact(request(3,status)).toString()).contains("처리 중");
        var other=request(3,status); other.withObject("member").withObject("user").put("id","other");
        assertThat(disconnected.interact(other).toString()).contains("본인의 요청");
        tasks.removeFirst().run();
        assertThat(disconnected.interact(request(3,status)).toString()).contains("등록 완료");
        disconnected.interact(request(3,status)); disconnected.interact(request(3,"official_confirm:"+id));
        verify(registration,times(1)).register(eq(7L),any());
    }

    @Test void queueRejectionKeepsDraftAndRegistrationFailureIsShown() {
        var accept=new java.util.concurrent.atomic.AtomicBoolean(false);
        var queued=new OfficialSongReview(registration,task->{if(!accept.get())throw new RejectedExecutionException();task.run();},(r,d)->replies.add(d));
        String id=start(queued); queued.interact(submit(id,"EX:45"));
        assertThat(queued.interact(request(3,"official_confirm:"+id)).toString()).contains("요청이 많습니다");
        verifyNoInteractions(registration);
        accept.set(true);
        when(registration.register(eq(7L),any())).thenThrow(new IllegalStateException("duplicate"));
        queued.interact(request(3,"official_confirm:"+id));
        assertThat(replies.getLast().toString()).contains("완료하지 못했습니다");
    }

    String start(OfficialSongReview service) {
        return mapper.valueToTree(service.start(request(3,"select"),report)).path("data").path("custom_id").asText().split(":")[1];
    }
    ObjectNode request(int type,String id) {
        var root=mapper.createObjectNode().put("type",type).put("guild_id","guild");
        root.withObject("member").withObject("user").put("id","admin");
        root.withObject("data").put("custom_id",id); return root;
    }
    ObjectNode submit(String id,String levels) {
        var root=request(5,"official_submit:"+id);
        var rows=root.withObject("data").withArray("components");
        for(var entry:Map.of("levels",levels,"version","29","upper","일반","gauge","없음","judgement","없음").entrySet())
            rows.addObject().put("type",1).withArray("components").addObject().put("custom_id",entry.getKey()).put("value",entry.getValue());
        return root;
    }
    void change(ObjectNode root,String id,String value) {
        for(var row:root.path("data").path("components")) {
            var component=(ObjectNode)row.path("components").get(0);
            if(component.path("custom_id").asText().equals(id)) component.put("value",value);
        }
    }
}
