package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ProblemDetail;

class DegradedModeEnricherTest {

  private DegradedModeEnricher enricher;
  private SystemLoadMonitor loadMonitor;
  private ApiStandardProperties properties;
  private ProblemDetail problemDetail;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    loadMonitor = mock(SystemLoadMonitor.class);
    properties = new ApiStandardProperties();
    enricher = new DegradedModeEnricher(loadMonitor, properties);
    problemDetail = ProblemDetail.forStatus(500);
    request = mock(HttpServletRequest.class);
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, true, true",   // highLoad, enabled, showInJson -> expectedAdded
    "true, true, false, false",
    "false, true, true, false",
    "true, false, true, false"
  })
  void testEnricher(boolean highLoad, boolean enabled, boolean showInJson, boolean expectedAdded) {
    // Given
    when(loadMonitor.isHighLoad()).thenReturn(highLoad);
    properties.getDegradedMode().setEnabled(enabled);
    properties.getDegradedMode().setShowInJson(showInJson);

    // When
    enricher.enrich(problemDetail, request, Locale.ENGLISH);

    // Then
    var props = problemDetail.getProperties();
    if (expectedAdded) {
      assertEquals(true, props.get("degraded"));
    } else {
      assertNull(props != null ? props.get("degraded") : null);
    }
  }
}
