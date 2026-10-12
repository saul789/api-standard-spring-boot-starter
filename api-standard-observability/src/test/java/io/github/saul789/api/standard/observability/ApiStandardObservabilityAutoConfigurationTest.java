package io.github.saul789.api.standard.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class ApiStandardObservabilityAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ApiStandardObservabilityAutoConfiguration.class));

  @Test
  void shouldRegisterMetricsEnricherWhenMeterRegistryIsPresent() {
    contextRunner
        .withUserConfiguration(MeterRegistryConfiguration.class)
        .run(context -> {
          assertThat(context).hasSingleBean(MetricsEnricher.class);
        });
  }

  @Test
  void shouldNotRegisterMetricsEnricherWhenMeterRegistryIsMissing() {
    contextRunner
        .run(context -> {
          assertThat(context).doesNotHaveBean(MetricsEnricher.class);
        });
  }

  @Test
  void shouldNotRegisterMetricsEnricherWhenPropertyIsDisabled() {
    contextRunner
        .withUserConfiguration(MeterRegistryConfiguration.class)
        .withPropertyValues("api.standard.metrics.enabled=false")
        .run(context -> {
          assertThat(context).doesNotHaveBean(MetricsEnricher.class);
        });
  }

  @Test
  void shouldRegisterOpenTelemetryProblemEnricher() {
    contextRunner
        .run(context -> {
          assertThat(context).hasSingleBean(OpenTelemetryProblemEnricher.class);
        });
  }

  @Test
  void shouldNotRegisterOpenTelemetryProblemEnricherWhenPropertyIsDisabled() {
    contextRunner
        .withPropertyValues("api.standard.enrichers.opentelemetry.enabled=false")
        .run(context -> {
          assertThat(context).doesNotHaveBean(OpenTelemetryProblemEnricher.class);
        });
  }

  @Configuration
  static class MeterRegistryConfiguration {
    @Bean
    MeterRegistry meterRegistry() {
      return mock(MeterRegistry.class);
    }
  }
}
