package gg.popn.http.discord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Metadata comes from the running image, not the latest branch or host .env. */
@Component
public class DeploymentVersion {
    private static final DateTimeFormatter KST = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'KST'")
            .withZone(ZoneId.of("Asia/Seoul"));
    private final String version;
    private final String revision;
    private final String builtAt;
    private final Instant startedAt = Instant.now();

    public DeploymentVersion(@Value("${POPNGG_RELEASE_VERSION:local}") String version,
                             @Value("${POPNGG_GIT_SHA:unknown}") String revision,
                             @Value("${POPNGG_BUILD_TIME:unknown}") String builtAt) {
        this.version = version;
        this.revision = revision;
        this.builtAt = builtAt;
    }

    public String message() {
        return "**현재 실행 중인 API 배포 버전**\n버전: `" + version
                + "`\n커밋: `" + revision + "`\n이미지 생성: " + buildTime()
                + "\n서버 시작: " + KST.format(startedAt)
                + "\n※ 이미지 생성 시각과 서버 시작 시각은 다를 수 있습니다.";
    }

    private String buildTime() {
        try {
            return KST.format(Instant.parse(builtAt));
        } catch (DateTimeParseException exception) {
            return "알 수 없음 (버전 정보 없는 빌드)";
        }
    }
}
