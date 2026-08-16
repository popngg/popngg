package gg.popn.http.common.config;

import gg.popn.infra.web.converter.StringToDifficultyConverter;
import gg.popn.infra.web.converter.StringToGenreNameConverter;
import gg.popn.infra.web.converter.StringToIsUpperConverter;
import gg.popn.infra.web.converter.StringToPoptomoIdConverter;
import gg.popn.infra.web.converter.StringToSongHashConverter;
import gg.popn.infra.web.converter.StringToSongNameConverter;
import gg.popn.infra.web.converter.StringToUserPopclassConverter;
import gg.popn.infra.web.converter.StringToUserRoleConverter;
import gg.popn.infra.web.converter.StringToUsernameConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins:https://popn.gg,https://www.popn.gg}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToSongHashConverter());
        registry.addConverter(new StringToDifficultyConverter());
        registry.addConverter(new StringToPoptomoIdConverter());
        registry.addConverter(new StringToGenreNameConverter());
        registry.addConverter(new StringToIsUpperConverter());
        registry.addConverter(new StringToSongNameConverter());
        registry.addConverter(new StringToUserPopclassConverter());
        registry.addConverter(new StringToUserRoleConverter());
        registry.addConverter(new StringToUsernameConverter());
    }
}
