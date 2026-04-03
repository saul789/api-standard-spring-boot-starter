package io.github.saul789.api.standard.exception;

import org.springframework.http.HttpStatus;
import java.net.URI;

/**
 * Signals a business-rule violation that should be communicated to the caller
 * with a specific HTTP status code and {@link ErrorCode}.
 *
 * <p>Throw this exception from service or domain code when an operation
 * cannot be completed due to a known, expected condition. It is handled by
 * {@link GlobalExceptionHandler} which converts it into
 * an RFC 9457 {@code ProblemDetail} response.
 */
public class BusinessException extends ProblemException {

    /**
     * Constructs a new business exception.
     *
     * @param code    the machine-readable error code
     * @param message the user-friendly detail message (or i18n key)
     * @param status  the HTTP status to return
     */
    public BusinessException(ErrorCode code, String message, HttpStatus status) {
        super(code, message, status);
    }

    /**
     * Constructs a new business exception.
     * The {@link ErrorCode} is automatically resolved from the HTTP status.
     *
     * @param message the user-friendly detail message (or i18n key)
     * @param status  the HTTP status to return
     */
    public BusinessException(String message, HttpStatus status) {
        super(message, status);
    }

    /**
     * Constructs a new business exception with a custom problem type URL.
     * The {@link ErrorCode} is automatically resolved from the HTTP status.
     *
     * @param message   the user-friendly detail message (or i18n key)
     * @param status    the HTTP status to return
     * @param customUrl the custom problem type URI (URL or URN)
     */
    public BusinessException(String message, HttpStatus status, String customUrl) {
        super(message, status, customUrl);
    }

    /**
     * Constructs a new business exception with a custom problem type URI string.
     *
     * @param code    the machine-readable error code
     * @param message the user-friendly detail message (or i18n key)
     * @param status  the HTTP status to return
     * @param type    the custom problem type URI (URL or URN)
     */
    public BusinessException(ErrorCode code, String message, HttpStatus status, String type) {
        super(code, message, status, type);
    }

    /**
     * Constructs a new business exception with a custom problem type URI object.
     *
     * @param code    the machine-readable error code
     * @param message the user-friendly detail message (or i18n key)
     * @param status  the HTTP status to return
     * @param type    the custom problem type URI
     */
    public BusinessException(ErrorCode code, String message, HttpStatus status, URI type) {
        super(code, message, status, type);
    }
}