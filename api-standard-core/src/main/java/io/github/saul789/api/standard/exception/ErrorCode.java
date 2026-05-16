package io.github.saul789.api.standard.exception;

/**
 * Definition of canonical error codes for the API.
 *
 * <p>These codes provide a machine-readable identifier for specific error conditions, allowing
 * clients to handle errors programmatically without relying on localized messages.
 */
public enum ErrorCode {
  /** Generic internal server error (500). */
  INTERNAL_ERROR,

  /** Client sent an invalid request or parameters (400). */
  BAD_REQUEST,

  /** Input validation failed (400, 422). */
  VALIDATION_ERROR,

  /** Resource is no longer available (410). */
  GONE,

  /** Authentication is required (401). */
  UNAUTHORIZED,

  /** Client lacks permissions for the resource (403). */
  FORBIDDEN,

  /** Resource not found (404). */
  NOT_FOUND,

  /** HTTP method not allowed for the URI (405). */
  METHOD_NOT_ALLOWED,

  /** Unsupported media type in request (415). */
  UNSUPPORTED_MEDIA_TYPE,

  /** Resource conflict or optimistic locking failure (409). */
  CONFLICT,

  /** Client has sent too many requests (429). */
  TOO_MANY_REQUESTS,

  /** Upstream server returned an invalid response (502). */
  BAD_GATEWAY,

  /** Upstream server is currently unavailable (503). */
  SERVICE_UNAVAILABLE,

  /** Upstream server timed out (504). */
  GATEWAY_TIMEOUT;

  /**
   * Resolves a sensible default {@link ErrorCode} based on an HTTP status code.
   *
   * @param status the HTTP status (e.g., 404, 500)
   * @return the most appropriate {@link ErrorCode}
   */
  public static ErrorCode fromStatus(int status) {
    return switch (status) {
      case 400 -> BAD_REQUEST;
      case 401 -> UNAUTHORIZED;
      case 403 -> FORBIDDEN;
      case 404 -> NOT_FOUND;
      case 405 -> METHOD_NOT_ALLOWED;
      case 409 -> CONFLICT;
      case 410 -> GONE;
      case 415 -> UNSUPPORTED_MEDIA_TYPE;
      case 422 -> VALIDATION_ERROR;
      case 429 -> TOO_MANY_REQUESTS;
      case 502 -> BAD_GATEWAY;
      case 503 -> SERVICE_UNAVAILABLE;
      case 504 -> GATEWAY_TIMEOUT;
      default -> (status >= 400 && status < 500) ? BAD_REQUEST : INTERNAL_ERROR;
    };
  }

  /**
   * Converts the enum name to kebab-case (e.g., INTERNAL_ERROR -> internal-error). Useful for
   * generating standardized URIs for problem types.
   *
   * @return the kebab-case version of the error code
   */
  public String toKebabCase() {
    return this.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }
}
