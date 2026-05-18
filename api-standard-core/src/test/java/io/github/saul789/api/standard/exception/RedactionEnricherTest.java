package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class RedactionEnricherTest {

  private RedactionEnricher enricher;
  private RedactionPolicy policy;
  private ApiStandardProperties properties;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    policy = mock(RedactionPolicy.class);
    properties = new ApiStandardProperties();
    enricher = new RedactionEnricher(policy, properties);
    request = mock(HttpServletRequest.class);
  }

  @Test
  void whenDisabled_shouldNotRedactAnyField() {
    // Given
    properties.getRedaction().setEnabled(false);
    ProblemDetail problem = buildProblemWithErrors("password", "secret123", "secret123");

    // When
    enricher.enrich(problem, request, Locale.ENGLISH);

    // Then - rejectedValue should remain unchanged
    var errors = getErrors(problem);
    assertEquals("secret123", errors.get(0).get("rejectedValue"));
  }

  @Test
  void whenFieldIsSensitive_shouldMaskRejectedValue() {
    // Given
    when(policy.shouldRedact("password")).thenReturn(true);
    ProblemDetail problem = buildProblemWithErrors("password", "mySecret");

    // When
    enricher.enrich(problem, request, Locale.ENGLISH);

    // Then
    var errors = getErrors(problem);
    assertEquals("[REDACTED]", errors.get(0).get("rejectedValue"));
  }

  @Test
  void whenFieldIsNotSensitive_shouldNotMaskRejectedValue() {
    // Given
    when(policy.shouldRedact("name")).thenReturn(false);
    ProblemDetail problem = buildProblemWithErrors("name", "John");

    // When
    enricher.enrich(problem, request, Locale.ENGLISH);

    // Then
    var errors = getErrors(problem);
    assertNull(errors.get(0).get("rejectedValue")); // no rejectedValue added
  }

  @Test
  void whenNoErrorsProperty_shouldNotThrow() {
    // Given
    ProblemDetail problem = ProblemDetail.forStatus(400);
    problem.setProperty("someOtherProp", "value");

    // When & Then
    assertDoesNotThrow(() -> enricher.enrich(problem, request, Locale.ENGLISH));
  }

  @Test
  void whenCustomMaskConfigured_shouldUseCustomMask() {
    // Given
    when(policy.shouldRedact("token")).thenReturn(true);
    properties.getRedaction().setMask("****");
    ProblemDetail problem = buildProblemWithErrors("token", "abc123");

    // When
    enricher.enrich(problem, request, Locale.ENGLISH);

    // Then
    var errors = getErrors(problem);
    assertEquals("****", errors.get(0).get("rejectedValue"));
  }

  @Test
  void orderShouldBeTwenty() {
    assertEquals(20, enricher.order());
  }

  // --- helpers ---

  private ProblemDetail buildProblemWithErrors(String field, String message) {
    ProblemDetail problem = ProblemDetail.forStatus(400);
    problem.setProperty("errors", List.of(Map.of("field", field, "message", message)));
    return problem;
  }

  private ProblemDetail buildProblemWithErrors(String field, String message, String rejectedValue) {
    ProblemDetail problem = ProblemDetail.forStatus(400);
    problem.setProperty("errors", List.of(Map.of("field", field, "message", message, "rejectedValue", rejectedValue)));
    return problem;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getErrors(ProblemDetail problem) {
    return (List<Map<String, Object>>) problem.getProperties().get("errors");
  }
}
