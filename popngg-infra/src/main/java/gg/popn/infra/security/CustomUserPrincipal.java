package gg.popn.infra.security;

import com.sun.security.auth.UserPrincipal;
import gg.popn.domain.user.model.User;
import gg.popn.domain.user.model.field.Password;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.domain.user.model.field.Username;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private final PoptomoId poptomoId;     // ✅ 식별자 VO
    private final Username username;
    private final UserRole role;           // ✅ 도메인 VO

    public CustomUserPrincipal(PoptomoId poptomoId, Username username, UserRole role) {
        this.poptomoId = poptomoId;
        this.username = username;
        this.role = role;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring 권장 ROLE_ 접두어
        if ("ADMIN".equals(role.getValue())) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("chart:create"),
                    new SimpleGrantedAuthority("chart:delete")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getPassword() { return ""; } // JWT라 사용 안 함
    @Override public String getUsername() { return username.getValue(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

}