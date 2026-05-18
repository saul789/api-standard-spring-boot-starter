package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class AnnotationEnricherTest {

  private final AnnotationEnricher enricher = new AnnotationEnricher();

  static class TestException extends RuntimeException {
    @ExposeToProblemDetail
    private final String orderId = "ORD-123";

    @ExposeToProblemDetail("custom_amount")
    private final double amount = 99.99;

  }

  @Test
  void shouldEnrichAnnotatedFieldsAndIgnoreOthers() {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    TestException ex = new TestException();

    enricher.enrich(problem, null, null, ex);

    assertEquals("ORD-123", problem.getProperties().get("orderId"));
    assertEquals(99.99, problem.getProperties().get("custom_amount"));

    // Unannotated fields should NOT be exposed
    assertFalse(problem.getProperties().containsKey("internalSecret"));
  }

  @Test
  void shouldHandleNullException() {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    enricher.enrich(problem, null, null, null);
    assertNull(problem.getProperties());
  }

  @Test
  void shouldHaveCorrectOrder() {
    assertEquals(10, enricher.order());
  }
}
