package io.github.saul789.api.standard;

import io.github.saul789.api.standard.exception.GlobalExceptionHandler;
import io.github.saul789.api.standard.filter.TraceContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Auto-configuration for API standard starter.
 * Uses @AutoConfiguration (preferred in Spring Boot 3+) instead
 * of @Configuration.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiStandardAutoConfiguration {

    /**
     * Filter to manage Trace ID in MDC for logging and RFC 9457 responses.
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceContextFilter traceContextFilter() {
        return new TraceContextFilter();
    }

    /**
     * Main exception handler. Requires MessageSource for i18n support.
     */
    @Bean
    @ConditionalOnMissingBean
    public GlobalExceptionHandler globalExceptionHandler(MessageSource messageSource) {
        return new GlobalExceptionHandler(messageSource);
    }

    /**
     * Bridges Spring's MessageSource with Hibernate Validator.
     * This makes @NotBlank(message =
     * "{jakarta.validation.constraints.NotBlank.message}") work.
     */
    @Bean
    @ConditionalOnMissingBean(MessageSource.class)
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        // Busca en src/main/resources/i18n/messages.properties
        messageSource.setBasename("i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");

        // IMPORTANTE: Evita que Spring use el idioma del servidor (ej. si el server
        // está en Linux/ES)
        // y lo obliga a usar el archivo base (messages.properties) si no hay match.
        messageSource.setFallbackToSystemLocale(false);

        return messageSource;
    }
}