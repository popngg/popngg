package gg.popn.http.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class HealthEndpointConfigurationTest {
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void productionPublicHealthPathIsAValidMainServerPath() throws Exception {
        assertPublicHealthPath("application-prod.yml");
    }

    @Test
    void localPublicHealthPathIsAValidMainServerPath() throws Exception {
        assertPublicHealthPath("application-local.yml");
    }

    private void assertPublicHealthPath(String resource) throws Exception {
        var properties = loader.load(resource, new ClassPathResource(resource)).getFirst();
        String configured = (String) properties.getProperty(
                "management.endpoint.health.group.public.additional-path");

        var path = AdditionalHealthEndpointPath.from(configured);

        assertThat(path.hasNamespace(WebServerNamespace.SERVER)).isTrue();
        assertThat(path.getValue()).isEqualTo("/health");
    }
}
