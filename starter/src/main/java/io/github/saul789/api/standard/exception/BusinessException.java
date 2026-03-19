package io.github.saul789.api.standard.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for business logic errors.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    public BusinessException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}