package io.github.saul789.api.standard.observability;

import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Enriches the API standard by automatically tracking error metrics in Micrometer.
 * It uses the 'api.errors.total' counter and tags it with code, status, method, and URI pattern.
 *
 * <p>To prevent cardinality explosion in metrics systems (like Prometheus), it uses the 
 * Spring MVC best matching pattern attribute instead of the raw request URI.
 */
public class MetricsEnricher implements ProblemDetailEnricher {

  private static final String UNKNOWN = "UNKNOWN";
  private final MeterRegistry registry;

  public MetricsEnricher(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale, Exception ex) {
    if (registry == null) {
      return;
    }
    
    String code = UNKNOWN;
    java.util.Map<String, Object> properties = problem.getProperties();
    if (properties != null) {
      Object codeObj = properties.get("code");
      if (codeObj != null) {
        code = codeObj.toString();
      }
    }

    String status = String.valueOf(problem.getStatus());
    String method = request != null ? request.getMethod() : UNKNOWN;

    String uri = UNKNOWN;
    if (request != null) {
      Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
      if (pattern != null) {
        uri = pattern.toString();
      } else {
        uri = "UNMAPPED"; // Safer than raw URI for unmapped paths like 404s
      }
    }

    registry
        .counter(
            "api.errors.total",
            List.of(
                Tag.of("code", code),
                Tag.of("status", status),
                Tag.of("method", method),
                Tag.of("uri", uri)))
        .increment();
  }

  @Override
  public int order() {
    return 999; // Run last to capture fully modified states
  }
}
