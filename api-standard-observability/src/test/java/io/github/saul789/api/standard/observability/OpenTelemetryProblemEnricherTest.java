package io.github.saul789.api.standard.observability;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class OpenTelemetryProblemEnricherTest {

  private OpenTelemetryProblemEnricher enricher;
  private HttpServletRequest request;
  private ProblemDetail problem;
  private MockedStatic<Span> mockedSpanStatic;
  private Span mockSpan;

  @BeforeEach
  void setUp() {
    enricher = new OpenTelemetryProblemEnricher();
    request = mock(HttpServletRequest.class);
    problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setProperty("code", "VALIDATION_ERROR");
    problem.setTitle("Bad Request");

    mockedSpanStatic = mockStatic(Span.class);
    mockSpan = mock(Span.class);
    mockedSpanStatic.when(Span::current).thenReturn(mockSpan);
  }

  @AfterEach
  void tearDown() {
    mockedSpanStatic.close();
  }

  @Test
  void shouldEnrichActiveAndRecordingSpan() {
    // Arrange
    when(mockSpan.isRecording()).thenReturn(true);
    Exception exception = new IllegalArgumentException("Invalid state");

    // Act
    enricher.enrich(problem, request, Locale.ENGLISH, exception);

    // Assert
    verify(mockSpan).setStatus(StatusCode.ERROR);
    verify(mockSpan).recordException(exception);
    verify(mockSpan).setAttribute("exception.type", IllegalArgumentException.class.getName());
    verify(mockSpan).setAttribute("exception.message", "Invalid state");
    verify(mockSpan).setAttribute("error.type", "VALIDATION_ERROR");
    verify(mockSpan).setAttribute("error.code", "VALIDATION_ERROR");
    verify(mockSpan).setAttribute("rfc9457.title", "Bad Request");
    verify(mockSpan).setAttribute("rfc9457.status", (long) HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void shouldNotEnrichIfSpanIsNotRecording() {
    // Arrange
    when(mockSpan.isRecording()).thenReturn(false);
    Exception exception = new IllegalArgumentException("Invalid state");

    // Act
    enricher.enrich(problem, request, Locale.ENGLISH, exception);

    // Assert
    verify(mockSpan, never()).setStatus(any());
    verify(mockSpan, never()).setAttribute(anyString(), anyString());
    verify(mockSpan, never()).setAttribute(anyString(), anyLong());
  }

  @Test
  void shouldHandleNullValuesGracefully() {
    // Arrange
    when(mockSpan.isRecording()).thenReturn(true);
    ProblemDetail mockProblem = mock(ProblemDetail.class);
    when(mockProblem.getStatus()).thenReturn(400);
    when(mockProblem.getTitle()).thenReturn(null);
    when(mockProblem.getProperties()).thenReturn(null);

    // Act & Assert
    assertDoesNotThrow(() -> enricher.enrich(mockProblem, request, Locale.ENGLISH, null));

    verify(mockSpan).setStatus(StatusCode.ERROR);
    verify(mockSpan, never()).setAttribute(eq("exception.type"), anyString());
    verify(mockSpan, never()).setAttribute(eq("exception.message"), anyString());
    verify(mockSpan, never()).setAttribute(eq("error.type"), anyString());
    verify(mockSpan, never()).setAttribute(eq("rfc9457.title"), anyString());
    verify(mockSpan).setAttribute("rfc9457.status", 400L);
  }
}
