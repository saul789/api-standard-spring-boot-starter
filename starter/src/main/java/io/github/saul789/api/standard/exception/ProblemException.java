package io.github.saul789.api.standard.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;

public class ProblemException extends RuntimeException implements ProblemTypeProvider {

  private final ErrorCode code;
  private final HttpStatus status;
  private final URI type;

  public ProblemException(ErrorCode code, String message, HttpStatus status) {
    this(code, message, status, (URI) null);
  }

  public ProblemException(ErrorCode code, String message, HttpStatus status, String customUrl) {
    this(code, message, status, customUrl != null ? URI.create(customUrl) : null);
  }

  public ProblemException(String customUrl, String message, HttpStatus status) {
    this(ErrorCode.fromStatus(status.value()), message, status, customUrl);
  }

  public ProblemException(String message, HttpStatus status, String customUrl) {
    this(ErrorCode.fromStatus(status.value()), message, status, customUrl);
  }

  public ProblemException(ErrorCode code, String message, HttpStatus status, URI type) {
    super(message);
    this.code = code;
    this.status = status;
    this.type = type;
  }

  public ProblemException(String message, HttpStatus status) {
    this(ErrorCode.fromStatus(status.value()), message, status, (URI) null);
  }

  public ErrorCode getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public URI getProblemType() {
    return type;
  }
}
