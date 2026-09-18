package io.github.saul789.api.standard.observability;

import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the observability extensions (metrics and telemetry tracing).
 */
@AutoConfiguration
public class ApiStandardObservabilityAutoConfiguration {

  /**
   * Registers the non-invasive OpenTelemetry problem enricher if OTel is on the classpath.
   */
  @Bean
  @ConditionalOnMissingBean(OpenTelemetryProblemEnricher.class)
  @ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
  @ConditionalOnProperty(prefix = "api.standard.enrichers.opentelemetry", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher openTelemetryProblemEnricher() {
    return new OpenTelemetryProblemEnricher();
  }

  /**
   * Registers the metrics enricher if Micrometer is on the classpath.
   */
  @Bean
  @ConditionalOnMissingBean(MetricsEnricher.class)
  @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
  @ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
  @ConditionalOnProperty(prefix = "api.standard.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher metricsEnricher(
      ObjectProvider<io.micrometer.core.instrument.MeterRegistry> registryProvider) {
    return new MetricsEnricher(registryProvider.getIfAvailable());
  }
}
