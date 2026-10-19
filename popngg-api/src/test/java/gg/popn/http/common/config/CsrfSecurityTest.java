package gg.popn.http.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.auth.port.out.IssuedAccessToken;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.http.common.JwtAuthenticationFilter;
import gg.popn.http.user.CsrfController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CsrfSecurityTest.TestApp.class)
@AutoConfigureMockMvc
class CsrfSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired TokenPort tokens;
    @BeforeEach void setUp() {
        reset(tokens);
        var principal = AuthPrincipal.of(PoptomoId.of("1234-5678-9012"), UserRole.of("USER"));
        when(tokens.parse("valid")).thenReturn(Optional.of(principal));
        when(tokens.parse("invalid")).thenReturn(Optional.empty());
        when(tokens.issueAccessToken(principal)).thenReturn(new IssuedAccessToken("renewed",120));
    }
    @Test void rejectsCookieWritesWithoutTrustedOriginOrToken() throws Exception {
        mvc.perform(post("/api/v1/write").cookie(new Cookie("access_token","valid"))).andExpect(status().isForbidden());
        for (String origin : new String[]{"https://evil.example","https://popn.gg.evil.example","https://evil.popn.gg","null"})
            mvc.perform(post("/api/v1/write").cookie(new Cookie("access_token","valid")).header("Origin",origin))
                    .andExpect(status().isForbidden());
    }
    @Test void permitsExistingBrowserAndBearerClientsButDoesNotFallBackOnCookie() throws Exception {
        mvc.perform(post("/api/v1/write").cookie(new Cookie("access_token","valid")).header("Origin","https://popn.gg"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/write").header("Authorization","Bearer valid")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/write").cookie(new Cookie("access_token","valid")).header("Authorization","Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
    @Test void protectsLoginBeforeAuthenticationAndAllowsOnlyExactDiscordPost() throws Exception {
        mvc.perform(post("/api/v1/auth/login")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login").header("Origin","https://popn.gg")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/discord/interactions").servletPath("/api/v1/discord/interactions"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/discord/interactions/extra").servletPath("/api/v1/discord/interactions/extra"))
                .andExpect(status().isForbidden());
    }
    @Test void issuesNonCacheableTokenAndValidatesCookieAndHeaderTogether() throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var json = new ObjectMapper().readTree(response.getContentAsString());
        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getDomain()).isNull();
        mvc.perform(post("/api/v1/write").cookie(cookie,new Cookie("access_token","valid"))
                .header(json.path("headerName").asText(),json.path("token").asText())).andExpect(status().isOk());
        mvc.perform(post("/api/v1/write").cookie(cookie,new Cookie("access_token","valid"))
                .header(json.path("headerName").asText(),"wrong")).andExpect(status().isForbidden());
    }
    @Test void originAllowlistRejectsAmbiguousOrWildcardOrigins() {
        for (String origin : new String[]{"*","null","https://popn.gg/","https://user@popn.gg","https://popn.gg?x=1","https://popn.gg#fragment"})
            assertThatThrownBy(() -> new ApiCsrfExemptions(origin)).isInstanceOf(IllegalArgumentException.class);
        var matcher = new ApiCsrfExemptions("https://popn.gg, https://www.popn.gg");
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.addHeader("Origin","https://www.popn.gg");
        assertThat(matcher.matches(request)).isTrue();
        request.addHeader("Origin","https://evil.example");
        assertThat(matcher.matches(request)).isFalse();
    }
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class,WebConfig.class,CsrfController.class,Beans.class,Endpoints.class})
    static class TestApp {}
    @TestConfiguration static class Beans {
        @Bean TokenPort tokenPort() { return mock(TokenPort.class); }
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(TokenPort tokens) { return new JwtAuthenticationFilter(tokens); }
    }
    @RestController static class Endpoints {
        @PostMapping({"/api/v1/write","/api/v1/auth/login","/api/v1/discord/interactions"}) String write() { return "ok"; }
    }
}
