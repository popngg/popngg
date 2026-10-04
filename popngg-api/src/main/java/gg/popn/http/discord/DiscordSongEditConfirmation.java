package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Component
public class DiscordSongEditConfirmation {
    private final Executor executor;
    private final Reply reply;

    @Autowired
    public DiscordSongEditConfirmation(ObjectMapper mapper) {
        this(new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), task -> {
            Thread thread = new Thread(task, "song-edit-confirmation");
            thread.setDaemon(true);
            return thread;
        }), new DiscordRatingReplyClient(mapper)::sendText);
    }

    DiscordSongEditConfirmation(Executor executor, Reply reply) {
        this.executor = executor;
        this.reply = reply;
    }

    Map<String, Object> start(JsonNode root, Supplier<Map<String, Object>> work) {
        try {
            executor.execute(() -> {
                String content;
                try {
                    Map<?, ?> data = (Map<?, ?>) work.get().get("data");
                    content = (String) data.get("content");
                } catch (Exception exception) {
                    content = "곡 수정 처리를 완료하지 못했습니다. 곡 조회로 저장 여부를 확인해 주세요.";
                }
                try { reply.send(root, content); }
                catch (Exception exception) {
                    LoggerFactory.getLogger(DiscordSongEditConfirmation.class)
                            .warn("Song edit reply delivery failed; check songId using song lookup");
                }
            });
            return Map.of("type", 5, "data", Map.of());
        } catch (RejectedExecutionException exception) {
            return Map.of("type", 4, "data", Map.of("content", "수정 요청이 많습니다. 잠시 후 수정 확정을 다시 눌러 주세요."));
        }
    }

    @PreDestroy
    void close() {
        if (executor instanceof ExecutorService service) service.shutdownNow();
    }

    @FunctionalInterface
    interface Reply { void send(JsonNode root, String content) throws Exception; }
}
