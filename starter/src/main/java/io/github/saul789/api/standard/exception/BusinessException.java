package io.github.saul789.api.standard.exception;

import org.springframework.http.HttpStatus;

/**
 * Signals a business-rule violation that should be communicated to the caller
 * with a specific HTTP status code and {@link ErrorCode}.
 *
 * <p>Throw this exception from service or domain code when an operation
 * cannot be completed due to a known, expected condition (e.g. duplicate
 * resource, insufficient balance). It is handled by
 * {@link GlobalExceptionHandler#handleBusiness} which converts it into
 * an RFC 9457 {@code ProblemDetail} response.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    /**
     * Constructs a new business exception.
     *
     * @param code    a machine-readable {@link ErrorCode} for API consumers
     * @param message a i18n message key or human-readable description;
     *                if a matching key exists in {@code messages.properties}
     *                it will be resolved to the request locale
     * @param status  the HTTP status to return to the caller
     */
    public BusinessException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /** Returns the machine-readable error code. */
    public ErrorCode getCode() {
        return code;
    }

    /** Returns the HTTP status to use in the response. */
    public HttpStatus getStatus() {
        return status;
    }
}