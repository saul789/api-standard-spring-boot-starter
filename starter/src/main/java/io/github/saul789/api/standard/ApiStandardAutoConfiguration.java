package io.github.saul789.api.standard;

import io.github.saul789.api.standard.exception.FeignExceptionHandler;
import io.github.saul789.api.standard.exception.GlobalExceptionHandler;
import io.github.saul789.api.standard.filter.RequestLoggingFilter;
import io.github.saul789.api.standard.filter.TraceContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Auto-configuration for the API standard starter.
 *
 * <p>Registers the following beans when running in a Servlet web application,
 * unless the application already provides its own:
 * <ul>
 *   <li>{@link TraceContextFilter} — W3C {@code traceparent} propagation and MDC binding</li>
 *   <li>{@link RequestLoggingFilter} — structured per-request access log</li>
 *   <li>{@link GlobalExceptionHandler} — RFC 9457 exception-to-response mapping</li>
 *   <li>{@link MessageSource} — i18n resolver backed by {@code i18n/messages} and
 *       {@code messages} resource bundles with UTF-8 encoding and no system-locale
 *       fallback</li>
 * </ul>
 *
 * <p>Uses {@link AutoConfiguration} (preferred over {@code @Configuration} in
 * Spring Boot 3+) and {@link ConditionalOnWebApplication} to remain inert in
 * non-web or reactive contexts.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
     * @return a fully configured {@link GlobalExceptionHandler}
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(MessageSource messageSource) {
        return new GlobalExceptionHandler(messageSource);
    }

    /**
     * Registers the Feign-specific exception handler if Feign is on the classpath.
     */
    @Bean
    @ConditionalOnMissingBean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(name = "feign.FeignException")
    public FeignExceptionHandler feignExceptionHandler() {
        return new FeignExceptionHandler();
    }

    /**
     * Registers a {@link MessageSource} backed by the {@code i18n/messages} and
     * {@code messages} resource bundles.
     *
     * <p>Only activated when the application does not already define a
     * {@code MessageSource} bean. System-locale fallback is disabled to ensure
     * the base bundle is always used when no locale-specific file matches.
     *
     * @return a UTF-8 {@link ResourceBundleMessageSource}
     */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages", "messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }
}