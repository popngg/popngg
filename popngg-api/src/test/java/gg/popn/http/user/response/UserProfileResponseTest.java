package gg.popn.http.user.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.user.dto.result.UserProfileResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserProfileResponseTest {

    @Test
    void convertsOnlyCurrentFormulaPopclassesToApiScale() {
        var response = UserProfileResponse.from(new UserProfileResult(
                "1234-5678-9012",
                "ポップ",
                "character",
                "comment",
                null,
                false,
                170_130,
                175_000,
                9_803,
                1,
                2,
                3,
                4));

        assertThat(response.displayPopclass()).isEqualTo(17_013);
        assertThat(response.potentialPopclass()).isEqualTo(17_500);
        assertThat(response.legacyPopclass()).isEqualTo(9_803);
    }

    @Test
    void publicResponseContainsNoAccountSecrets() throws Exception {
        var response = UserProfileResponse.from(new UserProfileResult(
                "1234-5678-9012",
                "ポップ",
                "character",
                "comment",
                null,
                false,
                100,
                200,
                300,
                1,
                2,
                3,
                4));

        String json = new ObjectMapper().writeValueAsString(response);

        assertThat(json)
                .doesNotContain("password")
                .doesNotContain("email")
                .doesNotContain("resetToken");
    }

    @Test
    void includesMedalSummariesAndUpdatedAt() {
        var updatedAt = LocalDateTime.of(2026, 8, 22, 12, 34, 56);
        var response = UserProfileResponse.from(new UserProfileResult(
                "1234-5678-9012",
                "ポップ",
                "character",
                "comment",
                null,
                false,
                100,
                200,
                300,
                1,
                2,
                3,
                4,
                List.of(
                        new UserProfileResult.MedalSummary("clear", 50, 3, 20),
                        new UserProfileResult.MedalSummary("full-combo", 49, 2, 30),
                        new UserProfileResult.MedalSummary("perfect", 48, 1, 40)),
                updatedAt));

        assertThat(response.medalSummaries())
                .containsExactly(
                        new UserProfileResponse.MedalSummaryResponse("clear", 50, 3, 20),
                        new UserProfileResponse.MedalSummaryResponse("full-combo", 49, 2, 30),
                        new UserProfileResponse.MedalSummaryResponse("perfect", 48, 1, 40));
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
