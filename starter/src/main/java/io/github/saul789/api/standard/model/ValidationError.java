package io.github.saul789.api.standard.model;

/**
 * Represents a validation error for a specific field.
 */
public class ValidationError {

    private String field;
    private String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}