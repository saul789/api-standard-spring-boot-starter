package io.github.saul789.api.standard.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValidationErrorTest {

  @Test
  void shouldStoreFieldAndMessage() {
    ValidationError err = new ValidationError("field", "msg");
    assertEquals("field", err.getField());
    assertEquals("msg", err.getMessage());
  }
}
