package io.github.saul789.api.standard.exception;

/**
 * Canonical error codes surfaced in API responses.
 *
 * <p>Each constant is serialised as-is into the {@code "code"} extension
 * property of an RFC 9457 {@code ProblemDetail} response, allowing API
 * consumers to branch on machine-readable values rather than HTTP status
 * codes or localised messages.
 */
public enum ErrorCode {

    /** An unexpected condition prevented the server from fulfilling the request. */
    INTERNAL_ERROR,

    /** One or more request fields failed constraint validation. */
    VALIDATION_ERROR,

    /** The request was syntactically or semantically invalid. */
    BAD_REQUEST,

    /** The requested resource could not be found. */
    NOT_FOUND
}