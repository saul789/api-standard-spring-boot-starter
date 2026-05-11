package io.github.saul789.api.standard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.saul789.api.standard.filter.RequestLoggingFilter;
import io.github.saul789.api.standard.filter.TraceContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class ApiStandardAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ApiStandardAutoConfiguration.class));

  @Test
  void shouldLoadAutoConfiguration() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(TraceContextFilter.class);
          assertThat(context).hasSingleBean(RequestLoggingFilter.class);
        });
  }
}
