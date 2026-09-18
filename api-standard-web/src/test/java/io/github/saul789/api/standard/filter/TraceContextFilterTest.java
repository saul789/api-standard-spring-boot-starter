package io.github.saul789.api.standard.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class TraceContextFilterTest {

  @InjectMocks
  private TraceContextFilter traceContextFilter;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  private boolean isValid(String value) {
    return (boolean) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
        traceContextFilter, "isValidTraceparent", value);
  }

  @Test
  void shouldReturnFalseWhenTraceparentIsNull() {
    assertFalse(isValid(null));
  }

  @Test
  void shouldReturnFalseWhenPartsAreInvalid() {
    assertFalse(isValid("00-abc"));
  }

  @Test
  void shouldReturnTrueWhenTraceparentIsValid() {
    String valid = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    assertTrue(isValid(valid));
  }

  @Test
  void shouldReturnFalseWhenTraceIdIsInvalid() {
    String invalidTraceId = "00-INVALID_TRACE_ID-00f067aa0ba902b7-01";
    assertFalse(isValid(invalidTraceId));
  }

  @Test
  void shouldReturnFalseWhenSpanIdIsInvalid() {
    String invalidSpanId = "00-4bf92f3577b34da6a3ce929d0e0e4736-INVALID_SPAN-01";
    assertFalse(isValid(invalidSpanId));
  }

  @Test
  void shouldReuseTraceIdFromExistingTraceparent() throws Exception {
    String existingTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
    String traceparent = "00-" + existingTraceId + "-00f067aa0ba902b7-01";

    when(request.getHeader("traceparent")).thenReturn(traceparent);

    doAnswer(
        invocation -> {
          assertEquals(existingTraceId, MDC.get("traceId"));
          return null;
        })
        .when(filterChain)
        .doFilter(request, response);

    traceContextFilter.doFilter(request, response, filterChain);

    verify(response, never()).setHeader(anyString(), anyString());

    assertNull(MDC.get("traceId"));
  }

  @ParameterizedTest
  @CsvSource({
      "traceId, agent-trace-id-12345",
      "trace_id, agent-trace-id-67890",
      "dd.trace_id, datadog-trace-id-abc",
      "trace.id, newrelic-trace-id-xyz",
      "dt.trace_id, dynatrace-trace-id-999"
  })
  void shouldReuseTraceIdFromExistingMdcKeys(String mdcKey, String expectedTraceId) throws Exception {
    MDC.put(mdcKey, expectedTraceId);

    when(request.getHeader("traceparent")).thenReturn(null);

    doAnswer(
        invocation -> {
          assertEquals(expectedTraceId, MDC.get("traceId"));
          return null;
        })
        .when(filterChain)
        .doFilter(request, response);

    traceContextFilter.doFilter(request, response, filterChain);

    verify(response, never()).setHeader(anyString(), anyString());
  }

  @Test
  void shouldGenerateNewTraceIdWhenHeaderIsMissing() throws ServletException, IOException {
    // Arrange: Null header
    when(request.getHeader("traceparent")).thenReturn(null);

    // Act
    traceContextFilter.doFilter(request, response, filterChain);

    // Assert
    verify(response, never()).setHeader(anyString(), anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldGenerateNewTraceIdWhenHeaderIsInvalid() throws ServletException, IOException {
    // Arrange: Header with incorrect format (less than 4 parts)
    when(request.getHeader("traceparent")).thenReturn("00-invalid-format");

    // Act
    traceContextFilter.doFilter(request, response, filterChain);

    // Assert: Should treat it as missing and generate a new one
    verify(response, never()).setHeader(anyString(), anyString());
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void shouldClearMDCEvenOnException() throws ServletException, IOException {
    // Arrange
    when(request.getHeader("traceparent")).thenReturn(null);
    doThrow(new RuntimeException("Filter error")).when(filterChain).doFilter(request, response);

    // Act and Assert
    assertThrows(
        RuntimeException.class, () -> traceContextFilter.doFilter(request, response, filterChain));

    // The finally block must clear the MDC
    assertNull(MDC.get("traceId"));
  }
}
