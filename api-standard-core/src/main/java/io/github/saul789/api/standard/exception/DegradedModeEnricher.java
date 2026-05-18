package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.ProblemDetail;

/**
 * Enriches the ProblemDetail with a 'degraded' flag when the system is under high load.
 */
public class DegradedModeEnricher implements ProblemDetailEnricher {

  private final SystemLoadMonitor loadMonitor;
  private final ApiStandardProperties properties;

  public DegradedModeEnricher(SystemLoadMonitor loadMonitor, ApiStandardProperties properties) {
    this.loadMonitor = loadMonitor;
    this.properties = properties;
  }

  @Override
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
    if (properties.getDegradedMode().isEnabled() && loadMonitor.isHighLoad()) {
      if (properties.getDegradedMode().isShowInJson()) {
        problem.setProperty("degraded", true);
      }
    }
  }

  @Override
  public int order() {
    return 10; // Run after standard enrichers
  }
}
