package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

class EnrichersTest {

  @Test
  void testTraceIdEnricher() {
    TraceIdEnricher enricher = new TraceIdEnricher();
    ProblemDetail problem = ProblemDetail.forStatus(500);
    HttpServletRequest request = mock(HttpServletRequest.class);

    // 1. Without traceId in MDC
    MDC.clear();
    enricher.enrich(problem, request, Locale.ENGLISH);
    if (problem.getProperties() != null) {
      assertNull(problem.getProperties().get("traceId"));
    }

    // 2. With traceId in MDC
    MDC.put("traceId", "test-trace");
    enricher.enrich(problem, request, Locale.ENGLISH);
    assertNotNull(problem.getProperties());
    assertEquals("test-trace", problem.getProperties().get("traceId"));
    MDC.clear();
  }

  @Test
  void testStandardMetadataEnricher() {
    StandardMetadataEnricher enricher = new StandardMetadataEnricher();
    ProblemDetail problem = ProblemDetail.forStatus(400);
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getRequestURI()).thenReturn("/api/test");

    enricher.enrich(problem, request, Locale.ENGLISH);
    assertEquals("/api/test", problem.getInstance().toString());

    // Test with invalid URI characters to hit catch block (if any, although getInstance usually
    // handles nulls)
    when(request.getRequestURI()).thenReturn("invalid uri with spaces");
    enricher.enrich(problem, request, Locale.ENGLISH);
    // Should not crash
  }
}
