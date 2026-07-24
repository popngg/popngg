package gg.popn.http.user.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.user.dto.result.UserProfileResult;
import org.junit.jupiter.api.Test;

class UserProfileResponseTest {

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
}
