package com.training.marketplace.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationYamlSecurityDefaultsTest {

    @Test
    void defaultApplicationConfig_doesNotEnableDebugSqlLogging() throws Exception {
        List<PropertySource<?>> propertySources = load("application.yml");

        assertThat(propertySources).allSatisfy(source -> {
            assertThat(source.getProperty("logging.level.org.hibernate.SQL")).isNull();
            assertThat(source.getProperty("logging.level.com.training.marketplace")).isNull();
        });
    }

    @Test
    void localApplicationConfig_enablesDebugLoggingOnlyAsAProfileOverride() throws Exception {
        List<PropertySource<?>> propertySources = load("application-local.yml");

        assertThat(propertySources).anySatisfy(source -> {
            assertThat(source.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("DEBUG");
            assertThat(source.getProperty("logging.level.com.training.marketplace")).isEqualTo("DEBUG");
        });
    }

    private List<PropertySource<?>> load(String resourceName) throws IOException {
        return new YamlPropertySourceLoader().load(
                resourceName, new ClassPathResource(resourceName));
    }
}
