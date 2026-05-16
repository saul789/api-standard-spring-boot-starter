package io.github.saul789.api.standard.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Enricher for timestamp and instance metadata. */
@Component
public class StandardMetadataEnricher implements ProblemDetailEnricher {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(StandardMetadataEnricher.class);

  @Override
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
    problem.setProperty("timestamp", Instant.now());
    try {
      problem.setInstance(URI.create(request.getRequestURI()));
    } catch (Exception e) {
      if (log.isTraceEnabled()) {
        log.trace("Failed to resolve request URI for problem instance: {}", e.getMessage());
      }
    }
  }

  @Override
  public int order() {
    return 0; // Run early
  }
}
