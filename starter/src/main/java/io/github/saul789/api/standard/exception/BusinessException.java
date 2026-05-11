package io.github.saul789.api.standard.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Signals a business-rule violation that should be communicated to the caller with a specific HTTP
 * status code and {@link ErrorCode}.
 *
 * <p>Throw this exception from service or domain code when an operation cannot be completed due to
 * a known, expected condition. It is handled by {@link GlobalExceptionHandler} which converts it
 * into an RFC 9457 {@code ProblemDetail} response.
 */
public class BusinessException extends ProblemException {

  /**
   * Constructs a new business exception.
   *
   * @param code the machine-readable error code
   * @param message the user-friendly detail message (or i18n key)
   * @param status the HTTP status to return
   */
  public BusinessException(ErrorCode code, String message, HttpStatus status) {
    super(code, message, status);
  }

  /**
   * Constructs a new business exception. The {@link ErrorCode} is automatically resolved from the
   * HTTP status.
   *
   * @param message the user-friendly detail message (or i18n key)
   * @param status the HTTP status to return
   */
  public BusinessException(String message, HttpStatus status) {
    super(message, status);
  }

  /**
   * Constructs a new business exception with a custom problem type URL. The {@link ErrorCode} is
   * automatically resolved from the HTTP status.
   *
   * @param message the user-friendly detail message (or i18n key)
   * @param status the HTTP status to return
   * @param customUrl the custom problem type URI (URL or URN)
   */
  public BusinessException(String message, HttpStatus status, String customUrl) {
    super(message, status, customUrl);
  }

  /**
   * Constructs a new business exception with a custom problem type URI string.
   *
   * @param code the machine-readable error code
   * @param message the user-friendly detail message (or i18n key)
   * @param status the HTTP status to return
   * @param type the custom problem type URI (URL or URN)
   */
  public BusinessException(ErrorCode code, String message, HttpStatus status, String type) {
    super(code, message, status, type);
  }

  /**
   * Constructs a new business exception with a custom problem type URI object.
   *
   * @param code the machine-readable error code
   * @param message the user-friendly detail message (or i18n key)
   * @param status the HTTP status to return
   * @param type the custom problem type URI
   */
  public BusinessException(ErrorCode code, String message, HttpStatus status, URI type) {
    super(code, message, status, type);
  }

  /**
   * Creates a new fluent builder for {@link BusinessException}.
   *
   * @param message the user-friendly detail message (or i18n key)
   * @return a new builder instance
   */
  public static Builder builder(String message) {
    return new Builder(message);
  }

  /** Fluent builder for {@link BusinessException}. */
  public static final class Builder {
    private final String message;
    private ErrorCode code;
    private HttpStatus status = HttpStatus.BAD_REQUEST;
    private URI type;

    private Builder(String message) {
      this.message = java.util.Objects.requireNonNull(message, "Message must not be null");
    }

    /** Sets the HTTP status. Defaults to 400 BAD_REQUEST. */
    public Builder status(HttpStatus status) {
      this.status = java.util.Objects.requireNonNull(status, "Status must not be null");
      return this;
    }

    /** Sets the explicit error code. If omitted, it's inferred from the status. */
    public Builder code(ErrorCode code) {
      this.code = java.util.Objects.requireNonNull(code, "ErrorCode must not be null");
      return this;
    }

    /** Sets a custom problem type URI. */
    public Builder type(URI type) {
      this.type = type;
      return this;
    }

    /** Sets a custom problem type URI from a string. */
    public Builder type(String type) {
      this.type = type != null ? URI.create(type) : null;
      return this;
    }

    /** Builds the exception instance. */
    public BusinessException build() {
      if (code == null) {
        if (type == null) {
          return new BusinessException(message, status);
        } else {
          return new BusinessException(message, status, type.toString());
        }
      } else {
        if (type == null) {
          return new BusinessException(code, message, status);
        } else {
          return new BusinessException(code, message, status, type);
        }
      }
    }
  }
}
