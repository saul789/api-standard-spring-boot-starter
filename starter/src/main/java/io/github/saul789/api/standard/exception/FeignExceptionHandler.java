package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;
import java.time.Instant;

/**
 * Exception handler that intercepts errors from Feign clients and converts them
 * to the API standard.
 *
 * <p>
 * Only activated if the project has Feign in the classpath.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(name = "feign.FeignException")
public class FeignExceptionHandler {
    private final ApiStandardProperties properties;
    private final org.springframework.context.MessageSource messageSource;

    public FeignExceptionHandler(ApiStandardProperties properties, org.springframework.context.MessageSource messageSource) {
        this.properties = properties;
        this.messageSource = messageSource;
    }

    /**
     * Handles exceptions from Feign clients and maps them to a local error code and
     * status.
     *
     * @param ex      the Feign exception
     * @param request the current request
     * @return the formatted ProblemDetail response
     */
    @ExceptionHandler(feign.FeignException.class)
    public ProblemDetail handleFeignException(feign.FeignException ex, HttpServletRequest request, java.util.Locale locale) {
        int externalStatus = ex.status();
        HttpStatus responseStatus = (externalStatus >= 400 && externalStatus < 500) ? HttpStatus.resolve(externalStatus)
                : HttpStatus.BAD_GATEWAY;
        if (responseStatus == null)
            responseStatus = HttpStatus.BAD_REQUEST;

        ErrorCode errorCode = ErrorCode.fromStatus(responseStatus.value());
        String codeName = errorCode.name();

        ProblemDetail problem = ProblemDetail.forStatus(responseStatus);
        
        String titleKey = "error." + codeName;
        String defaultTitle = responseStatus.getReasonPhrase();
        problem.setTitle(messageSource.getMessage(titleKey, null, defaultTitle, locale));

        String detailKey = responseStatus.is4xxClientError() ? "error.feign.client" : "error.feign.failure";
        String defaultDetail = responseStatus.is4xxClientError() 
                ? "Upstream service reported client-side error: " + ex.getMessage() 
                : "Upstream service reported server-side failure";
        
        problem.setDetail(messageSource.getMessage(detailKey, null, defaultDetail, locale));
        problem.setProperty("code", codeName);

        if (properties.getErrors().getTypeOverrides().containsKey(codeName)) {
            try {
                problem.setType(URI.create(properties.getErrors().getTypeOverrides().get(codeName)));
            } catch (Exception _) {
                problem.setType(generateDefaultType(codeName));
            }
        } else {
            problem.setType(generateDefaultType(codeName));
        }

        try {
            problem.setInstance(URI.create(request.getRequestURI()));
        } catch (Exception _) {
            // Standard problem detail instantiation fallback
        }
        problem.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null)
            problem.setProperty("traceId", traceId);

        return problem;
    }

    private URI generateDefaultType(String code) {
        String baseUri = properties.getErrors().getTypeBaseUri();
        String typeSuffix;
        try {
            typeSuffix = ErrorCode.valueOf(code).toKebabCase();
        } catch (Exception _) {
            typeSuffix = code.toLowerCase().replace('_', '-');
        }
        return URI.create(baseUri + typeSuffix);
    }
}
