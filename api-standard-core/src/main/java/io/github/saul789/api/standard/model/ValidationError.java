package io.github.saul789.api.standard.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single field-level validation failure.
 *
 * <p>Instances are embedded in a {@code ProblemDetail} under the {@code "errors"} extension
 * property whenever a validation exception (e.g. {@link
 * jakarta.validation.ConstraintViolationException}) is handled.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

  private String field;
  private String message;

  /**
   * Constructs a validation error for the given field.
   *
   * @param field the dot-notation path of the offending field (e.g. {@code "user.email"})
   * @param message a human-readable description of the violation
   */
  public ValidationError(String field, String message) {
    this.field = field;
    this.message = message;
  }

  /** Returns the dot-notation path of the offending field. */
  public String getField() {
    return field;
  }

  /** Returns the human-readable description of the violation. */
  public String getMessage() {
    return message;
  }
}
