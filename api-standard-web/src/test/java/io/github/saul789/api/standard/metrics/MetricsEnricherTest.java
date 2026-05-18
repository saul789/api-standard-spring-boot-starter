package io.github.saul789.api.standard.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerMapping;

class MetricsEnricherTest {

  private MeterRegistry registry;
  private Counter counter;
  private MetricsEnricher enricher;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    registry = mock(MeterRegistry.class);
    counter = mock(Counter.class);
    request = mock(HttpServletRequest.class);
    enricher = new MetricsEnricher(registry);
  }

  @Test
  void shouldIncrementCounterWithAllTags() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setProperty("code", "VALIDATION_ERROR");

    when(request.getMethod()).thenReturn("POST");
    when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn("/users/{id}");
    
    List<Tag> expectedTags = List.of(
        Tag.of("code", "VALIDATION_ERROR"),
        Tag.of("status", "400"),
        Tag.of("method", "POST"),
        Tag.of("uri", "/users/{id}")
    );
    
    when(registry.counter("api.errors.total", expectedTags)).thenReturn(counter);

    // Act
    enricher.enrich(problem, request, null, null);

    // Assert
    verify(counter).increment();
  }

  @Test
  void shouldHandleMissingInformationGracefully() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    
    List<Tag> expectedTags = List.of(
        Tag.of("code", "UNKNOWN"),
        Tag.of("status", "500"),
        Tag.of("method", "UNKNOWN"),
        Tag.of("uri", "UNKNOWN")
    );
    
    when(registry.counter("api.errors.total", expectedTags)).thenReturn(counter);

    // Act
    enricher.enrich(problem, null, null, null);

    // Assert
    verify(counter).increment();
  }

  @Test
  void shouldHandleUnmappedUriGracefully() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    
    when(request.getMethod()).thenReturn("GET");
    when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn(null);
    
    List<Tag> expectedTags = List.of(
        Tag.of("code", "UNKNOWN"),
        Tag.of("status", "404"),
        Tag.of("method", "GET"),
        Tag.of("uri", "UNMAPPED")
    );
    
    when(registry.counter("api.errors.total", expectedTags)).thenReturn(counter);

    // Act
    enricher.enrich(problem, request, null, null);

    // Assert
    verify(counter).increment();
  }

  @Test
  void shouldHaveOrder999() {
    assertEquals(999, enricher.order());
  }
}
