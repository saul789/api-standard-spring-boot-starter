package io.github.saul789.api.standard;

import io.github.saul789.api.standard.filter.TraceContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ApiStandardAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ApiStandardAutoConfiguration.class));

    @Test
    void shouldLoadAutoConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(TraceContextFilter.class);
        });
    }
}