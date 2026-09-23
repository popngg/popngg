package gg.popn.http.analysis;

import gg.popn.application.analysis.AnalysisJobs;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.http.common.JwtAuthenticationFilter;
import gg.popn.http.common.config.SecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=AnalysisControllerTest.App.class)
@AutoConfigureMockMvc
class AnalysisControllerTest {
    @Autowired MockMvc mvc; @Autowired AnalysisJobs jobs; @Autowired TokenPort tokens;
    @BeforeEach void init(){reset(jobs,tokens);for(String role:List.of("USER","ADMIN"))when(tokens.parse(role)).thenReturn(Optional.of(AuthPrincipal.of(PoptomoId.of("0000-0000-0000"),UserRole.from(role))));}
    @Test void requiresAdminAndReturns202WithoutRunningAnalysis()throws Exception {
        mvc.perform(post("/api/v1/admin/analysis/cpi-spi")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/analysis/cpi-spi").header("Authorization","Bearer USER")).andExpect(status().isForbidden());
        verifyNoInteractions(jobs);
        when(jobs.submit("ADMIN","admin:req1")).thenReturn(new AnalysisJobs.Submission("job","QUEUED",false));
        mvc.perform(post("/api/v1/admin/analysis/cpi-spi").header("Authorization","Bearer ADMIN").header("Idempotency-Key","req1"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.jobId").value("job"));
        mvc.perform(get("/api/v1/admin/analysis/cpi-spi/job").header("Authorization","Bearer USER")).andExpect(status().isForbidden());
    }
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class,AnalysisController.class})
    static class App {
        @Bean AnalysisJobs jobs(){return mock(AnalysisJobs.class);}
        @Bean TokenPort tokens(){return mock(TokenPort.class);}
        @Bean JwtAuthenticationFilter filter(TokenPort port){return new JwtAuthenticationFilter(port);}
    }
}
