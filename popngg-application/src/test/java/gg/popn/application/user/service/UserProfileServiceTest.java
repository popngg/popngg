package gg.popn.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.auth.port.out.CurrentPrincipalPort;
import gg.popn.application.user.dto.command.UpdateUserProfileCommand;
import gg.popn.application.user.dto.query.UserProfileQuery;
import gg.popn.application.user.dto.query.UserRankingQuery;
import gg.popn.application.user.dto.result.UserProfileResult;
import gg.popn.application.user.exception.UserProfileNotFoundException;
import gg.popn.application.user.port.out.UserProfilePort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    private static final String TARGET = "1234-5678-9012";

    @Mock
    private UserProfilePort profilePort;
    @Mock
    private CurrentPrincipalPort currentPrincipalPort;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(profilePort, currentPrincipalPort);
    }

    @Test
    void publicProfileKeepsTheThreePopclassMeaningsSeparate() {
        var profile = profile(false);
        when(profilePort.findByPoptomoId(TARGET)).thenReturn(Optional.of(profile));

        var result = service.get(new UserProfileQuery(TARGET));

        assertThat(result.displayPopclass()).isEqualTo(100);
        assertThat(result.potentialPopclass()).isEqualTo(200);
        assertThat(result.legacyPopclass()).isEqualTo(300);
    }

    @Test
    void hiddenProfileIsNotExposedToAnonymousCallers() {
        when(profilePort.findByPoptomoId(TARGET)).thenReturn(Optional.of(profile(true)));
        when(currentPrincipalPort.get()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new UserProfileQuery(TARGET)))
                .isInstanceOf(UserProfileNotFoundException.class);
    }

    @Test
    void ownerCanUpdateTheirProfile() {
        var command = new UpdateUserProfileCommand(
                TARGET, null, null, "updated", null, false);
        when(currentPrincipalPort.get()).thenReturn(Optional.of(principal(TARGET, "USER")));
        when(profilePort.findByPoptomoId(TARGET)).thenReturn(Optional.of(profile(false)));
        when(profilePort.update(command)).thenReturn(profile(false));

        service.update(command);

        verify(profilePort).update(command);
    }

    @Test
    void anotherUserCannotUpdateTheProfile() {
        var command = new UpdateUserProfileCommand(
                TARGET, null, null, "updated", null, false);
        when(currentPrincipalPort.get())
                .thenReturn(Optional.of(principal("9999-9999-9999", "USER")));

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(AccessDeniedException.class);
        verify(profilePort, never()).update(command);
    }

    @Test
    void administratorCanUpdateAnotherProfile() {
        var command = new UpdateUserProfileCommand(
                TARGET, null, null, "updated", null, false);
        when(currentPrincipalPort.get())
                .thenReturn(Optional.of(principal("9999-9999-9999", "ADMIN")));
        when(profilePort.findByPoptomoId(TARGET)).thenReturn(Optional.of(profile(false)));
        when(profilePort.update(command)).thenReturn(profile(false));

        service.update(command);

        verify(profilePort).update(command);
    }

    @Test
    void rankingsReturnOnlyThePortFilteredPage() {
        var query = new UserRankingQuery(
                UserRankingQuery.Sort.POTENTIAL_POPCLASS, 0, 20);
        when(profilePort.findRankings(query))
                .thenReturn(new UserProfilePort.RankingPage(List.of(profile(false)), 1));

        var result = service.rankings(query);

        assertThat(result.users()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    private static AuthPrincipal principal(String poptomoId, String role) {
        return AuthPrincipal.of(PoptomoId.of(poptomoId), UserRole.of(role));
    }

    private static UserProfileResult profile(boolean hidden) {
        return new UserProfileResult(
                TARGET,
                "ポップ",
                "character",
                "comment",
                null,
                hidden,
                100,
                200,
                300,
                1,
                2,
                3,
                4);
    }
}
