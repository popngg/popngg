package gg.popn.http.discord;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentVersionTest {
    @Test
    void handlesLocalBuildWithoutMetadata() {
        assertThat(new DeploymentVersion("local", "unknown", "unknown").message())
                .contains("local", "unknown", "알 수 없음", "서버 시작");
    }

    @Test
    void convertsImageTimestampToKoreanDate() {
        assertThat(new DeploymentVersion("2026.09.03.010000-abcdef0", "abcdef0",
                "2026-09-02T16:00:00Z").message()).contains("2026-09-03 01:00:00 KST");
    }
}
