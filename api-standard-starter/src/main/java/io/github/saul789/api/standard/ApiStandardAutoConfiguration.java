package io.github.saul789.api.standard;

import io.github.saul789.api.standard.exception.FeignExceptionHandler;
import io.github.saul789.api.standard.exception.GlobalExceptionHandler;
import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import io.github.saul789.api.standard.exception.ProblemDetailService;
import io.github.saul789.api.standard.exception.StandardMetadataEnricher;
import io.github.saul789.api.standard.exception.TraceIdEnricher;
import io.github.saul789.api.standard.exception.DefaultRedactionPolicy;
import io.github.saul789.api.standard.exception.DefaultSystemLoadMonitor;
import io.github.saul789.api.standard.exception.DegradedModeEnricher;
import io.github.saul789.api.standard.exception.RedactionEnricher;
import io.github.saul789.api.standard.exception.RedactionPolicy;
import io.github.saul789.api.standard.exception.SystemLoadMonitor;
import io.github.saul789.api.standard.filter.DegradedModeFilter;
import io.github.saul789.api.standard.exception.TranslationValidator;
import io.github.saul789.api.standard.filter.RequestLoggingFilter;
import io.github.saul789.api.standard.filter.TraceContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Auto-configuration for the API standard starter.
 *
 * <p>
 * Registers the following beans when running in a Servlet web application,
 * unless the
 * application already provides its own:
 *
 * <ul>
 * <li>{@link TraceContextFilter} — W3C {@code traceparent} propagation and MDC
 * binding
 * <li>{@link RequestLoggingFilter} — structured per-request access log
 * <li>{@link GlobalExceptionHandler} — RFC 9457 exception-to-response mapping
 * <li>{@link MessageSource} — i18n resolver backed by {@code i18n/messages} and
 * {@code messages}
 * resource bundles
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ApiStandardProperties.class)
public class ApiStandardAutoConfiguration {

  /** Registers the W3C Trace Context filter. */
  @Bean
  @ConditionalOnMissingBean
  public TraceContextFilter traceContextFilter() {
    return new TraceContextFilter();
  }

  /** Registers the structured request-logging filter. */
  @Bean
  @ConditionalOnMissingBean
  public RequestLoggingFilter requestLoggingFilter() {
    return new RequestLoggingFilter();
  }

  /**
   * Registers the global exception handler.
   *
   * @param messageSource        the i18n source used for title and detail
   *                             resolution
   * @param problemDetailService the service for building RFC 9457 responses
   * @return a fully configured {@link GlobalExceptionHandler}
   */
  @Bean
  @ConditionalOnMissingBean(GlobalExceptionHandler.class)
  public GlobalExceptionHandler globalExceptionHandler(
      MessageSource messageSource, ProblemDetailService problemDetailService) {
    return new GlobalExceptionHandler(messageSource, problemDetailService);
  }

  /**
   * Registers the ProblemDetail service.
   *
   * @param messageSource the i18n source
   * @param properties    configuration properties for the starter
   * @param enrichers     list of registered enrichers
   * @return a fully configured {@link ProblemDetailService}
   */
  @Bean
  @ConditionalOnMissingBean(ProblemDetailService.class)
  public ProblemDetailService problemDetailService(
      MessageSource messageSource,
      ApiStandardProperties properties,
      java.util.List<ProblemDetailEnricher> enrichers) {
    return new ProblemDetailService(messageSource, properties, enrichers);
  }

  /**
   * Registers the traceId enricher. Disable with
   * api.standard.enrichers.trace-id.enabled=false
   */
  @Bean
  @ConditionalOnMissingBean(TraceIdEnricher.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.enrichers.trace-id", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher traceIdEnricher() {
    return new TraceIdEnricher();
  }

  /**
   * Registers the standard metadata enricher. Disable with
   * api.standard.enrichers.metadata.enabled=false
   */
  @Bean
  @ConditionalOnMissingBean(StandardMetadataEnricher.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.enrichers.metadata", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher standardMetadataEnricher() {
    return new StandardMetadataEnricher();
  }
  /**
   * Registers the Feign-specific exception handler if Feign is on the classpath.
   *
   * @param properties    configuration properties for the starter
   * @param messageSource the i18n source
   * @return a fully configured {@link FeignExceptionHandler}
   */
  @Bean
  @ConditionalOnMissingBean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "feign.FeignException")
  public FeignExceptionHandler feignExceptionHandler(
      ApiStandardProperties properties, MessageSource messageSource) {
    return new FeignExceptionHandler(properties, messageSource);
  }

  /**
   * Registers a {@link MessageSource} backed by the {@code i18n/messages} and
   * {@code messages}
   * resource bundles.
   *
   * @return a UTF-8 {@link ResourceBundleMessageSource}
   */
  @Bean
  @Primary
  public MessageSource apiStandardMessageSource() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasenames("i18n/messages", "messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setFallbackToSystemLocale(false);
    return messageSource;
  }

  /**
   * Registers the Actuator endpoint if spring-boot-actuator is present on the
   * classpath.
   *
   * @param properties configuration properties for the starter
   * @return the {@link io.github.saul789.api.standard.actuator.ApiErrorsEndpoint}
   */
  @Bean
  @ConditionalOnMissingBean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
  public io.github.saul789.api.standard.actuator.ApiErrorsEndpoint apiErrorsEndpoint(
      ApiStandardProperties properties) {
    return new io.github.saul789.api.standard.actuator.ApiErrorsEndpoint(properties);
  }

  /** Registers the system load monitor. */
  @Bean
  @ConditionalOnMissingBean
  public SystemLoadMonitor systemLoadMonitor(ApiStandardProperties properties) {
    return new DefaultSystemLoadMonitor(properties);
  }

  /**
   * Registers the degraded mode enricher. Disable with
   * api.standard.enrichers.degraded-mode.enabled=false
   */
  @Bean
  @ConditionalOnMissingBean(DegradedModeEnricher.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.enrichers.degraded-mode", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher degradedModeEnricher(
      SystemLoadMonitor loadMonitor, ApiStandardProperties properties) {
    return new DegradedModeEnricher(loadMonitor, properties);
  }

  /**
   * Registers the degraded mode filter. Disable with
   * api.standard.degraded-mode.show-in-header=false
   */
  @Bean
  @ConditionalOnMissingBean(DegradedModeFilter.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.degraded-mode", name = "show-in-header", havingValue = "true", matchIfMissing = true)
  public DegradedModeFilter degradedModeFilter(
      SystemLoadMonitor loadMonitor, ApiStandardProperties properties) {
    return new DegradedModeFilter(loadMonitor, properties);
  }

  /**
   * Registers the default redaction policy (reads from YML). Provide your own
   * bean to override.
   */
  @Bean
  @ConditionalOnMissingBean(RedactionPolicy.class)
  public RedactionPolicy defaultRedactionPolicy(ApiStandardProperties properties) {
    return new DefaultRedactionPolicy(properties);
  }

  /**
   * Registers the redaction enricher. Disable with
   * api.standard.enrichers.redaction.enabled=false
   */
  @Bean
  @ConditionalOnMissingBean(RedactionEnricher.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.enrichers.redaction", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher redactionEnricher(
      RedactionPolicy redactionPolicy, ApiStandardProperties properties) {
    return new RedactionEnricher(redactionPolicy, properties);
  }

  /**
   * Registers the annotation enricher. Disable with
   * api.standard.enrichers.annotation.enabled=false
   */
  @Bean
  @ConditionalOnMissingBean(io.github.saul789.api.standard.exception.AnnotationEnricher.class)
  @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "api.standard.enrichers.annotation", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ProblemDetailEnricher annotationEnricher() {
    return new io.github.saul789.api.standard.exception.AnnotationEnricher();
  }
}
