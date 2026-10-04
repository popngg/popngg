package gg.popn.http.analysis;

import gg.popn.application.analysis.AchievementConstants;
import gg.popn.application.analysis.AchievementConstantArchive;
import gg.popn.application.auth.port.out.TokenPort;
import gg.popn.domain.user.model.AuthPrincipal;
import gg.popn.domain.user.model.field.PoptomoId;
import gg.popn.domain.user.model.field.UserRole;
import gg.popn.http.common.JwtAuthenticationFilter;
import gg.popn.http.common.config.SecurityConfig;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=AchievementConstantControllerTest.App.class)
@AutoConfigureMockMvc
class AchievementConstantControllerTest {
    @Autowired MockMvc mvc;@Autowired AchievementConstants constants;@Autowired AchievementConstantArchive archive;@Autowired TokenPort tokens;
    @BeforeEach void init(){reset(constants,archive,tokens);for(String role:List.of("USER","ADMIN"))when(tokens.parse(role)).thenReturn(
            Optional.of(AuthPrincipal.of(PoptomoId.of("0000-0000-0000"),UserRole.from(role))));}
    @Test void importsOnlyForAdmin()throws Exception{
        String body=body();
        mvc.perform(post("/api/v1/admin/analysis/achievement-constants/snapshots").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/analysis/achievement-constants/snapshots").header("Authorization","Bearer USER")
                .contentType("application/json").content(body)).andExpect(status().isForbidden());
        when(constants.importSnapshot(any())).thenReturn(new AchievementConstants.Stored(1,"source:1",AchievementConstants.Axis.MEDAL,1));
        when(archive.archive(any())).thenReturn("s3://private/artifact.json");
        mvc.perform(post("/api/v1/admin/analysis/achievement-constants/snapshots").header("Authorization","Bearer ADMIN")
                .contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotId").value(1)).andExpect(jsonPath("$.artifact").value("s3://private/artifact.json"));
    }
    @Test void doesNotActivateDatabaseSnapshotWhenArchiveFails()throws Exception{
        doThrow(new IllegalStateException("S3 unavailable")).when(archive).archive(any());
        mvc.perform(post("/api/v1/admin/analysis/achievement-constants/snapshots").header("Authorization","Bearer ADMIN")
                .contentType("application/json").content(body())).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ACHIEVEMENT_ARCHIVE_UNAVAILABLE"));
        verify(constants,never()).importSnapshot(any());
    }
    private static String body(){return """
                {"sourceSnapshotId":"source:1","modelVersion":"achievement-v1","modelStatus":"EXPERIMENTAL",
                 "generatedAt":"2026-09-25T00:00:00Z","axis":"MEDAL","constants":[
                   {"chartId":1,"songName":"song","level":49,"axis":"medal","target":"CLEAR",
                    "rawDifficulty":1.2,"difficultyConstant":49.25,"interval":[49.0,49.5],
                    "playerCount":100,"achievedCount":50,"status":"EXPERIMENTAL","holdReasons":[]}]}
                """;
    }
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class,FlywayAutoConfiguration.class})
    @Import({SecurityConfig.class,AchievementConstantController.class})
    static class App{
        @Bean AchievementConstants constants(){return mock(AchievementConstants.class);}
        @Bean AchievementConstantArchive archive(){return mock(AchievementConstantArchive.class);}
        @Bean TokenPort tokens(){return mock(TokenPort.class);}
        @Bean JwtAuthenticationFilter filter(TokenPort port){return new JwtAuthenticationFilter(port);}
    }
}
