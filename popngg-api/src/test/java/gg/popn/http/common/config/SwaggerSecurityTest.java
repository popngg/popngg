package gg.popn.http.common.config;

import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.http.common.JwtAuthenticationFilter;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = SwaggerSecurityTest.TestApp.class,
        properties = {
                "popngg.swagger.username=docs-admin",
                "popngg.swagger.password=secret-password"
        })
@AutoConfigureMockMvc
class SwaggerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void swaggerRequiresBasicAuthentication() throws Exception {
        mvc.perform(get("/swagger-ui/test")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui/test")
                        .header("Authorization", "Basic ZG9jcy1hZG1pbjpzZWNyZXQtcGFzc3dvcmQ="))
                .andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/test")
                        .header("Authorization", "Basic ZG9jcy1hZG1pbjpzZWNyZXQtcGFzc3dvcmQ="))
                .andExpect(status().isOk());
    }

    @Test
    void publicUserApiRemainsPublic() throws Exception {
        mvc.perform(get("/api/v1/users/test")).andExpect(status().isOk());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfig.class, Beans.class, Endpoints.class})
    static class TestApp {
    }

    @TestConfiguration
    static class Beans {
        @Bean
        TokenPort tokenPort() {
            return mock(TokenPort.class);
        }

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(TokenPort tokenPort) {
            return new JwtAuthenticationFilter(tokenPort);
        }
    }

    @RestController
    static class Endpoints {
        @GetMapping({"/swagger-ui/test", "/v3/api-docs/test", "/api/v1/users/test"})
        String ok() {
            return "ok";
        }
    }
}
