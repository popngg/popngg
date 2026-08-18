package gg.popn.http.user;

import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserRankingResult;
import gg.popn.application.user.port.in.UserProfileUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserPaginationControllerTest {
    @Test
    void mapsUserRankingToCommonPageResponse() {
        var profiles = mock(UserProfileUseCase.class);
        var user = new UserProfileResult(
                "1234-5678-9012", "name", "character", "comment", null, false,
                10, 20, 30, 1, 2, 3, 4);
        when(profiles.rankings(any())).thenReturn(
                new UserRankingResult(List.of(user), 0, 20, 21));
        var controller = new UserController(profiles);

        var page = controller.getUserRankings("displayPopclass", 0, 20).getData();

        assertThat(page.items()).singleElement().satisfies(profile ->
                assertThat(profile.poptomoId()).isEqualTo("1234-5678-9012"));
        assertThat(page.totalItems()).isEqualTo(21);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.hasPrev()).isFalse();
        assertThat(page.hasNext()).isTrue();
    }
}
