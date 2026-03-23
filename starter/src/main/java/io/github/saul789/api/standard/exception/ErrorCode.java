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
    NOT_FOUND,

    /** The HTTP method is not supported for this endpoint. */
    METHOD_NOT_ALLOWED,

    /** The media type is not supported. */
    UNSUPPORTED_MEDIA_TYPE,

    /** Client is not authenticated. */
    UNAUTHORIZED,

    /** Client is authenticated but lacks required permissions. */
    FORBIDDEN;

    /**
     * Resolves a default {@link ErrorCode} based on an HTTP status code.
     *
     * @param status the HTTP status to resolve from
     * @return the most appropriate {@link ErrorCode}
     */
    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            default -> (status >= 400 && status < 500) ? BAD_REQUEST : INTERNAL_ERROR;
        };
    }
}