package gg.popn.http.user;

import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.dto.result.UserRankingResult;
import gg.popn.application.user.dto.result.UserListResult;
import gg.popn.application.user.dto.result.UserProfileResult.MedalSummary;
import gg.popn.application.user.port.in.UserProfileUseCase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDateTime;

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

    @Test
    void mapsFrontendUsersPageWithOneBasedPagination() {
        var profiles = mock(UserProfileUseCase.class);
        var item = new UserListResult.Item(
                "1234-5678-9012", "name", null, "comment", 3, 177000,
                List.of(new MedalSummary("clear", 50, 1, 1),
                        new MedalSummary("full-combo", 0, 0, 0),
                        new MedalSummary("perfect", 48, 1, 1)),
                LocalDateTime.of(2026, 8, 23, 1, 2));
        when(profiles.findUsers(any())).thenReturn(
                new UserListResult(List.of(item), 0, 20, 21));

        var page = new UserController(profiles)
                .findUsers(null, "rank", "asc", 1, 20).getData();

        assertThat(page.items()).singleElement().satisfies(user -> {
            assertThat(user.id()).isEqualTo("1234-5678-9012");
            assertThat(user.name()).isEqualTo("name");
            assertThat(user.bestLevels().get(1).maxLevel()).isNull();
        });
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
