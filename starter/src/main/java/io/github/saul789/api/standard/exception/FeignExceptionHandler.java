package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Separate controller advice for OpenFeign communication errors.
 *
 * <p>Only activated when {@code feign.FeignException} is present on the
 * classpath ({@code spring-cloud-starter-openfeign} optional dependency),
 * keeping the core starter free of a mandatory Feign dependency.
 *
 * <p><strong>Mapping strategy:</strong>
 * <ul>
 *   <li>4xx upstream errors are propagated with the same status code so the
 *       caller knows the request itself was at fault.</li>
 *   <li>5xx upstream errors and unrecognised status codes are mapped to
 *       {@code 502 Bad Gateway} to avoid leaking internal service details.</li>
 * </ul>
 */
@RestControllerAdvice
@ConditionalOnClass(name = "feign.FeignException")
public class FeignExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FeignExceptionHandler.class);

    /**
     * Converts a Feign communication failure into a standardised error response.
     *
     * @param ex      the Feign exception carrying the upstream HTTP status
     * @param request the current HTTP request, used to populate the response path
     * @return a {@code 4xx} or {@code 502} response wrapped in an {@link ApiResponse}
     */
    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleFeignException(feign.FeignException ex,
            HttpServletRequest request) {
        int externalStatus = ex.status();
        HttpStatus responseStatus;
        String errorMessage;

        if (externalStatus >= 400 && externalStatus < 500) {
            responseStatus = HttpStatus.resolve(externalStatus);
            if (responseStatus == null) {
                responseStatus = HttpStatus.BAD_REQUEST;
            }
            errorMessage = "External client error: " + ex.getMessage();
        } else {
            responseStatus = HttpStatus.BAD_GATEWAY;
            errorMessage = "External service failure (Upstream error)";
        }

        log.error("Feign communication failed. External Status: {}, Target Path: {}",
                externalStatus, request.getRequestURI());

        ApiResponse<Object> errorResponse = ApiResponse.error(
                errorMessage,
                request.getRequestURI());

        return ResponseEntity.status(responseStatus).body(errorResponse);
    }
}
