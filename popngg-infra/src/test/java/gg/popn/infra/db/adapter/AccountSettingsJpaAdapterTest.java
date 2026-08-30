package gg.popn.infra.db.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gg.popn.application.account.exception.AccountSettingsException;
import gg.popn.infra.db.entity.UserEntity;
import gg.popn.infra.db.entity.UserProfileEntity;
import gg.popn.infra.db.jpa.UserJpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountSettingsJpaAdapterTest {
    private final UserJpaRepository users = mock(UserJpaRepository.class);
    private final AccountSettingsJpaAdapter adapter = new AccountSettingsJpaAdapter(users);
    private UserEntity user;

    @BeforeEach
    void setUp() {
        var profile = UserProfileEntity.builder().userId(1L).userName("name")
                .characterName("character").comment("old").profileImageUrl("old-avatar")
                .hidden(false).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        user = UserEntity.builder().id(1L).poptomoId("1234-5678-9012")
                .passwordHash("old-hash").role("USER").createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now()).profile(profile).build();
        when(users.findByPoptomoId("1234-5678-9012")).thenReturn(Optional.of(user));
    }

    @Test
    void findsAndUpdatesSettingsIncludingAvatarRemoval() {
        assertThat(adapter.find("1234-5678-9012").avatarUrl()).isEqualTo("old-avatar");

        var updated = adapter.updateProfile("1234-5678-9012", "new", true, null, true);

        assertThat(updated.comment()).isEqualTo("new");
        assertThat(updated.privateProfile()).isTrue();
        assertThat(updated.avatarUrl()).isNull();
        verify(users).save(user);
    }

    @Test
    void retainsAvatarWhenItWasNotChanged() {
        var updated = adapter.updateProfile("1234-5678-9012", "new", false, null, false);
        assertThat(updated.avatarUrl()).isEqualTo("old-avatar");
    }

    @Test
    void readsAndChangesPasswordHash() {
        assertThat(adapter.passwordHash("1234-5678-9012")).isEqualTo("old-hash");
        adapter.updatePasswordHash("1234-5678-9012", "new-hash");
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(users).save(user);
    }

    @Test
    void mapsMissingAccountToUnauthenticated() {
        when(users.findByPoptomoId("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adapter.find("missing"))
                .isInstanceOfSatisfying(AccountSettingsException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("UNAUTHENTICATED");
                });
    }
}
