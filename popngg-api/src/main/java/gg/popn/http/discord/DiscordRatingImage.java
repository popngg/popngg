package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.playdata.port.in.PlaydataQueryUseCase;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.user.port.in.UserProfileUseCase;
import gg.popn.http.analysis.RatingController;
import gg.popn.http.analysis.TierListImageRenderer;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class DiscordRatingImage {
    private final RatingQuery ratings;
    private final PlaydataQueryUseCase playdata;
    private final UserProfileUseCase profiles;
    private final TierListImageRenderer renderer;
    private final Executor executor;
    private final Reply reply;

    @Autowired
    public DiscordRatingImage(RatingQuery ratings, PlaydataQueryUseCase playdata, UserProfileUseCase profiles,
                              TierListImageRenderer renderer, ObjectMapper mapper) {
        this(ratings, playdata, profiles, renderer,
                new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(8), runnable -> {
                    Thread thread = new Thread(runnable, "discord-rating-image");
                    thread.setDaemon(true);
                    return thread;
                }), new DiscordRatingReplyClient(mapper)::send);
    }

    DiscordRatingImage(RatingQuery ratings, PlaydataQueryUseCase playdata, UserProfileUseCase profiles,
                       TierListImageRenderer renderer, Executor executor, Reply reply) {
        this.ratings = ratings;
        this.playdata = playdata;
        this.profiles = profiles;
        this.renderer = renderer;
        this.executor = executor;
        this.reply = reply;
    }

    public Map<String, Object> start(JsonNode root, int level, String metricValue, String poptomoId) {
        if (level < 48 || level > 50) return message("지원 레벨은 48, 49, 50입니다.");
        String normalizedId = poptomoId == null ? "" : poptomoId.strip();
        if (!normalizedId.matches("^\\d{4}-\\d{4}-\\d{4}$"))
            return message("팝토모 ID를 `1234-5678-9012` 형식으로 입력해 주세요.");
        final RatingController.Metric metric;
        try { metric = RatingController.Metric.valueOf(metricValue.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException exception) { return message("기준은 CPI 또는 SPI만 선택할 수 있습니다."); }

        try {
            executor.execute(() -> render(root, level, metric, normalizedId));
            // The deferred acknowledgement determines whether the final edited reply is public.
            // Keep successful tier-list requests public so everyone in the channel can view them.
            return Map.of("type", 5, "data", Map.of());
        } catch (RejectedExecutionException exception) {
            return message("이미지 생성 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void render(JsonNode root, int level, RatingController.Metric metric, String poptomoId) {
        Result result;
        try {
            var snapshot = ratings.latest();
            var user = playdata.findUserPlaydata(poptomoId);
            var profile = profiles.get(new UserProfileQuery(poptomoId));
            byte[] png = renderer.render(snapshot, user, profile, level, metric);
            String filename = "popngg-lv%d-%s-%s.png".formatted(
                    level, metric.name().toLowerCase(Locale.ROOT), poptomoId);
            String content = "**%s님의 Lv%d %s 서열표**\n점수와 메달은 popn.gg 최고 기록 기준입니다. 현재 모델은 실험 단계입니다."
                    .formatted(user.userName(), level, metric);
            result = new Result(content, filename, png);
        } catch (IllegalArgumentException | UserProfileNotFoundException exception) {
            result = Result.text("해당 팝토모 ID의 사용자를 찾을 수 없습니다.");
        } catch (IllegalStateException exception) {
            result = Result.text("CPI/SPI 데이터가 아직 준비되지 않았습니다. 분석 최신화가 완료된 뒤 다시 시도해 주세요.");
        } catch (Exception exception) {
            result = Result.text("서열표 이미지를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        try { reply.send(root, result); }
        catch (Exception ignored) {
            // Never log interaction tokens or URLs.
        }
    }

    private static Map<String, Object> message(String content) {
        return Map.of("type", 4, "data", Map.of("content", content, "flags", 64,
                "allowed_mentions", Map.of("parse", List.of())));
    }

    @PreDestroy
    void close() {
        if (executor instanceof ExecutorService service) service.shutdownNow();
    }

    record Result(String content, String filename, byte[] png) {
        static Result text(String content) { return new Result(content, null, null); }
        boolean hasImage() { return filename != null && png != null; }
    }

    @FunctionalInterface
    interface Reply {
        void send(JsonNode root, Result result) throws Exception;
    }
}
