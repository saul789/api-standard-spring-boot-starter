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
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FeignExceptionHandler.class);
    private final ApiStandardProperties properties;
    private final org.springframework.context.MessageSource messageSource;

    private static final com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>> MAP_TYPE = new com.fasterxml.jackson.core.type.TypeReference<>() {};
    
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
        HttpStatus responseStatus = resolveHttpStatus(ex.status());
        ErrorCode errorCode = ErrorCode.fromStatus(responseStatus.value());
        String codeName = errorCode.name();

        ProblemDetail problem = ProblemDetail.forStatus(responseStatus);
        problem.setTitle(resolveTitle(codeName, responseStatus, locale));
        
        String finalizedDetail = resolveInitialDetail(responseStatus, ex, locale);
        
        // Try to parse the remote error body if present
        RemoteErrorDetails remoteDetails = extractRemoteErrorDetails(ex);
        if (remoteDetails != null) {
            if (remoteDetails.detail() != null) finalizedDetail = remoteDetails.detail();
            if (remoteDetails.code() != null) codeName = remoteDetails.code();
        }

        problem.setDetail(finalizedDetail);
        problem.setProperty("code", codeName);

        if (properties.getErrors().getTypeOverrides().containsKey(codeName)) {
            try {
                problem.setType(URI.create(properties.getErrors().getTypeOverrides().get(codeName)));
            } catch (Exception e) {
                log.debug("Invalid override URI for {}: {}", codeName, e.getMessage());
                problem.setType(generateDefaultType(codeName));
            }
        } else {
            problem.setType(generateDefaultType(codeName));
        }

        try {
            problem.setInstance(URI.create(request.getRequestURI()));
        } catch (Exception e) {
            log.trace("Failed to resolve request URI: {}", e.getMessage());
        }
        problem.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null)
            problem.setProperty("traceId", traceId);

        return problem;
    }

    private HttpStatus resolveHttpStatus(int externalStatus) {
        HttpStatus status = (externalStatus >= 400 && externalStatus < 500) ? HttpStatus.resolve(externalStatus)
                : HttpStatus.BAD_GATEWAY;
        return status != null ? status : HttpStatus.BAD_REQUEST;
    }

    private String resolveTitle(String codeName, HttpStatus status, java.util.Locale locale) {
        String titleKey = "error." + codeName;
        String defaultTitle = status.getReasonPhrase();
        return messageSource.getMessage(titleKey, null, defaultTitle, locale);
    }

    private String resolveInitialDetail(HttpStatus status, feign.FeignException ex, java.util.Locale locale) {
        String detailKey = status.is4xxClientError() ? "error.feign.client" : "error.feign.failure";
        String fallbackDetail = status.is4xxClientError() 
                ? "Upstream service reported client-side error: " + ex.getMessage() 
                : "Upstream service reported server-side failure";
        return messageSource.getMessage(detailKey, null, fallbackDetail, locale);
    }

    private RemoteErrorDetails extractRemoteErrorDetails(feign.FeignException ex) {
        if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                java.util.Map<String, Object> remoteBody = mapper.readValue(ex.contentUTF8(), MAP_TYPE);
                
                String detail = (String) remoteBody.get("detail");
                String code = (String) remoteBody.get("code");
                return new RemoteErrorDetails(detail, code);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.debug("Failed to parse Feign error body: {}", e.getMessage());
            }
        }
        return null;
    }

    private record RemoteErrorDetails(String detail, String code) {}

    private URI generateDefaultType(String code) {
        String baseUri = properties.getErrors().getTypeBaseUri();
        String typeSuffix;
        try {
            typeSuffix = ErrorCode.valueOf(code).toKebabCase();
        } catch (Exception ignored) {
            typeSuffix = code.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
        }
        return URI.create(baseUri + typeSuffix);
    }
}
