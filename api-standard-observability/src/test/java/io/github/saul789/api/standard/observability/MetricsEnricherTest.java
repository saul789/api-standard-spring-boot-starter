package io.github.saul789.api.standard.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerMapping;

class MetricsEnricherTest {

  private MeterRegistry registry;
  private MetricsEnricher enricher;
  private Counter counter;

  @BeforeEach
  void setUp() {
    registry = mock(MeterRegistry.class);
    enricher = new MetricsEnricher(registry);
    counter = mock(Counter.class);
    
    when(registry.counter(anyString(), anyList())).thenReturn(counter);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldRecordMetricsWithCorrectTags() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setProperty("code", "VALIDATION_ERROR");
    
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("POST");
    when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
        .thenReturn("/api/users/{id}");

    // Act
    enricher.enrich(problem, request, Locale.ENGLISH, null);

    // Assert
    ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
    verify(registry).counter(eq("api.errors.total"), tagsCaptor.capture());
    verify(counter).increment();

    List<Tag> tags = tagsCaptor.getValue();
    assertEquals(4, tags.size());
    assertTag(tags, "code", "VALIDATION_ERROR");
    assertTag(tags, "status", "400");
    assertTag(tags, "method", "POST");
    assertTag(tags, "uri", "/api/users/{id}");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldFallbackGracefullyForMissingAttributes() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    
    // Act
    enricher.enrich(problem, null, Locale.ENGLISH, null);

    // Assert
    ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
    verify(registry).counter(eq("api.errors.total"), tagsCaptor.capture());
    verify(counter).increment();

    List<Tag> tags = tagsCaptor.getValue();
    assertTag(tags, "code", "UNKNOWN");
    assertTag(tags, "status", "500");
    assertTag(tags, "method", "UNKNOWN");
    assertTag(tags, "uri", "UNKNOWN");
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldHandleUnmappedRoutesGracefully() {
    // Arrange
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn(null);

    // Act
    enricher.enrich(problem, request, Locale.ENGLISH, null);

    // Assert
    ArgumentCaptor<List<Tag>> tagsCaptor = ArgumentCaptor.forClass(List.class);
    verify(registry).counter(eq("api.errors.total"), tagsCaptor.capture());
    verify(counter).increment();

    List<Tag> tags = tagsCaptor.getValue();
    assertTag(tags, "uri", "UNMAPPED");
  }

  private void assertTag(List<Tag> tags, String key, String value) {
    Tag matched = tags.stream()
        .filter(t -> t.getKey().equals(key))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Tag key " + key + " not found"));
    assertEquals(value, matched.getValue());
  }
}
