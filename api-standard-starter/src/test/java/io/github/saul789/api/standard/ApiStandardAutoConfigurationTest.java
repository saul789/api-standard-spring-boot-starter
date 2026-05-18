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
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.GlobalExceptionHandler.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.ProblemDetailService.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.TraceIdEnricher.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.StandardMetadataEnricher.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.FeignExceptionHandler.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.actuator.ApiErrorsEndpoint.class);
          assertThat(context).hasBean("apiStandardMessageSource");
          
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.DegradedModeEnricher.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.RedactionEnricher.class);
          assertThat(context).hasSingleBean(io.github.saul789.api.standard.exception.AnnotationEnricher.class);
          
          // MetricsEnricher should NOT be present without MeterRegistry
          assertThat(context).doesNotHaveBean(io.github.saul789.api.standard.metrics.MetricsEnricher.class);
        });
  }

  @Test
  void shouldLoadMetricsEnricherWhenMeterRegistryIsPresent() {
    contextRunner
        .withBean(io.micrometer.core.instrument.MeterRegistry.class, () -> org.mockito.Mockito.mock(io.micrometer.core.instrument.MeterRegistry.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(io.github.saul789.api.standard.metrics.MetricsEnricher.class);
            });
  }
}
