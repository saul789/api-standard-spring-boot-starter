package io.github.saul789.sample.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Excepción personalizada decorada con @ResponseStatus. Resultado: 409 Conflict. */
@ResponseStatus(HttpStatus.CONFLICT)
public class CustomConflictException extends RuntimeException {
  public CustomConflictException(String message) {
    super(message);
  }
}
