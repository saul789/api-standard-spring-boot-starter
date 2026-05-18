package io.github.saul789.api.standard.filter;

import static org.mockito.Mockito.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import io.github.saul789.api.standard.exception.SystemLoadMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DegradedModeFilterTest {

  private DegradedModeFilter filter;
  private SystemLoadMonitor loadMonitor;
  private ApiStandardProperties properties;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    loadMonitor = mock(SystemLoadMonitor.class);
    properties = new ApiStandardProperties();
    filter = new DegradedModeFilter(loadMonitor, properties);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, true, true",   // highLoad, enabled, showInHeader -> expectedAdded
    "true, true, false, false",
    "false, true, true, false",
    "true, false, true, false"
  })
  void testFilter(boolean highLoad, boolean enabled, boolean showInHeader, boolean expectedAdded) throws ServletException, IOException {
    // Given
    when(loadMonitor.isHighLoad()).thenReturn(highLoad);
    properties.getDegradedMode().setEnabled(enabled);
    properties.getDegradedMode().setShowInHeader(showInHeader);

    // When
    filter.doFilterInternal(request, response, filterChain);

    // Then
    if (expectedAdded) {
      verify(response).setHeader("X-API-Degraded", "true");
    } else {
      verify(response, never()).setHeader(anyString(), anyString());
    }
    verify(filterChain).doFilter(request, response);
  }
}
