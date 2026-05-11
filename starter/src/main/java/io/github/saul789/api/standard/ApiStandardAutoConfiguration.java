package io.github.saul789.api.standard;

import io.github.saul789.api.standard.exception.*;
import io.github.saul789.api.standard.exception.FeignExceptionHandler;
import io.github.saul789.api.standard.exception.GlobalExceptionHandler;
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
 * <p>Registers the following beans when running in a Servlet web application, unless the
 * application already provides its own:
 *
 * <ul>
 *   <li>{@link TraceContextFilter} — W3C {@code traceparent} propagation and MDC binding
 *   <li>{@link RequestLoggingFilter} — structured per-request access log
 *   <li>{@link GlobalExceptionHandler} — RFC 9457 exception-to-response mapping
 *   <li>{@link MessageSource} — i18n resolver backed by {@code i18n/messages} and {@code messages}
 *       resource bundles
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
   * @param messageSource the i18n source used for title and detail resolution
   * @param properties configuration properties for the starter
   * @return a fully configured {@link GlobalExceptionHandler}
   */
  @Bean
  @ConditionalOnMissingBean(GlobalExceptionHandler.class)
  public GlobalExceptionHandler globalExceptionHandler(
      MessageSource messageSource, ProblemDetailService problemDetailService) {
    return new GlobalExceptionHandler(messageSource, problemDetailService);
  }

  @Bean
  @ConditionalOnMissingBean(ProblemDetailService.class)
  public ProblemDetailService problemDetailService(
      MessageSource messageSource,
      ApiStandardProperties properties,
      java.util.List<ProblemDetailEnricher> enrichers) {
    return new ProblemDetailService(messageSource, properties, enrichers);
  }

  @Bean
  public ProblemDetailEnricher traceIdEnricher() {
    return new TraceIdEnricher();
  }

  @Bean
  public ProblemDetailEnricher standardMetadataEnricher() {
    return new StandardMetadataEnricher();
  }

  /**
   * Registers the Feign-specific exception handler if Feign is on the classpath.
   *
   * @param properties configuration properties for the starter
   * @return a fully configured {@link FeignExceptionHandler}
   */
  @Bean
  @ConditionalOnMissingBean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
      name = "feign.FeignException")
  public FeignExceptionHandler feignExceptionHandler(
      ApiStandardProperties properties, MessageSource messageSource) {
    return new FeignExceptionHandler(properties, messageSource);
  }

  /**
   * Registers a {@link MessageSource} backed by the {@code i18n/messages} and {@code messages}
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
   * Registers the Actuator endpoint if spring-boot-actuator is present on the classpath.
   *
   * @param properties configuration properties for the starter
   * @return the {@link io.github.saul789.api.standard.actuator.ApiErrorsEndpoint}
   */
  @Bean
  @ConditionalOnMissingBean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(
      name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
  public io.github.saul789.api.standard.actuator.ApiErrorsEndpoint apiErrorsEndpoint(
      ApiStandardProperties properties) {
    return new io.github.saul789.api.standard.actuator.ApiErrorsEndpoint(properties);
  }
}
