package io.github.saul789.api.standard.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Separate controller advice for OpenFeign communication errors.
 *
 * <p>Only activated when {@code feign.FeignException} is present on the
 * classpath ({@code spring-cloud-starter-openfeign} optional dependency).
 *
 * <p>Uses {@link Ordered#HIGHEST_PRECEDENCE} to ensure Feign-specific
 * exceptions are caught here before falling back to the generic
 * GlobalExceptionHandler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(name = "feign.FeignException")
public class FeignExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FeignExceptionHandler.class);

    /**
     * Converts a Feign communication failure into a standardised RFC 9457 
     * ProblemDetail response.
     *
     * @param ex      the Feign exception carrying the upstream HTTP status
     * @param request the current HTTP request, used to populate the response path
     * @return an RFC 9457 problem detail with the mapped status
     */
    @ExceptionHandler(feign.FeignException.class)
    public ProblemDetail handleFeignException(feign.FeignException ex, HttpServletRequest request) {
        int externalStatus = ex.status();
        HttpStatus responseStatus;
        boolean isClientError = externalStatus >= 400 && externalStatus < 500;

        if (isClientError) {
            responseStatus = HttpStatus.resolve(externalStatus);
            if (responseStatus == null) {
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.BAD_GATEWAY;
        }

        log.error("Feign communication failed. External Status: {}, Internal Resolved Status: {}, Path: {}",
                externalStatus, responseStatus, request.getRequestURI());

        ProblemDetail problem = ProblemDetail.forStatus(responseStatus);
        problem.setTitle(responseStatus.getReasonPhrase());
        problem.setDetail(isClientError ? "External client error: " + ex.getMessage() 
                                      : "External service failure (Upstream error)");
        
        // Standard extensions
        problem.setProperty("code", ErrorCode.fromStatus(responseStatus.value()).name());
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }

        return problem;
    }
}
