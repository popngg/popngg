package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstants;
import gg.popn.http.analysis.AchievementConstantImageRenderer;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class DiscordAchievementConstantImage {
    private final AchievementConstants constants;
    private final AchievementConstantImageRenderer renderer;
    private final Executor executor;
    private final Reply reply;

    @Autowired
    public DiscordAchievementConstantImage(AchievementConstants constants,
            AchievementConstantImageRenderer renderer, ObjectMapper mapper) {
        this(constants, renderer, new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), runnable -> {
                    Thread thread=new Thread(runnable,"discord-achievement-image");thread.setDaemon(true);return thread;
                }), new DiscordRatingReplyClient(mapper)::send);
    }

    DiscordAchievementConstantImage(AchievementConstants constants,
            AchievementConstantImageRenderer renderer, Executor executor, Reply reply) {
        this.constants=constants;this.renderer=renderer;this.executor=executor;this.reply=reply;
    }

    public Map<String,Object> start(JsonNode root,int level,String axisValue) {
        if(level<48||level>50)return message("지원 레벨은 48, 49, 50입니다.");
        final AchievementConstants.Axis axis;
        try{axis=AchievementConstants.Axis.valueOf(axisValue.toUpperCase(Locale.ROOT));}
        catch(RuntimeException exception){return message("기준은 메달 또는 랭크만 선택할 수 있습니다.");}
        try{
            executor.execute(()->render(root,level,axis));
            return Map.of("type",5,"data",Map.of("flags",64));
        }catch(RejectedExecutionException exception){return message("이미지 생성 요청이 많습니다. 잠시 후 다시 시도해 주세요.");}
    }

    private void render(JsonNode root,int level,AchievementConstants.Axis axis){
        Result result;
        try{
            var snapshot=constants.latest(level,axis);
            byte[] png=renderer.render(snapshot);
            String label=axis==AchievementConstants.Axis.MEDAL?"메달":"스코어 랭크";
            result=new Result("**Lv%d %s 달성 난이도 상수표**\n관리자 검토용 실험 자료입니다."
                    .formatted(level,label),"popngg-lv%d-%s-constants.png".formatted(
                    level,axis.name().toLowerCase(Locale.ROOT)),png);
        }catch(IllegalStateException exception){result=Result.text("상수 데이터가 아직 저장되지 않았습니다. 실험 결과를 먼저 등록해 주세요.");}
        catch(Exception exception){result=Result.text("상수표 이미지를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.");}
        try{reply.send(root,result);}catch(Exception ignored){ }
    }

    private static Map<String,Object> message(String content){return Map.of("type",4,"data",Map.of(
            "content",content,"flags",64,"allowed_mentions",Map.of("parse",List.of())));}
    @PreDestroy void close(){if(executor instanceof ExecutorService service)service.shutdownNow();}
    record Result(String content,String filename,byte[] png){static Result text(String content){return new Result(content,null,null);}
        boolean hasImage(){return filename!=null&&png!=null;}}
    @FunctionalInterface interface Reply{void send(JsonNode root,Result result)throws Exception;}
}
