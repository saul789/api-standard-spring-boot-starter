package io.github.saul789.api.standard.observability;

import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Non-invasive OpenTelemetry (OTel) Problem Enricher.
 * Respects OTel standards by recording exception metadata on the current active span,
 * while remaining fully optional and safe if the OTel library is not present on the classpath.
 */
@Component
@ConditionalOnClass(name = "io.opentelemetry.api.trace.Span")
public class OpenTelemetryProblemEnricher implements ProblemDetailEnricher {

  @Override
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale, Exception ex) {
    Span activeSpan = Span.current();
    if (activeSpan != null && activeSpan.isRecording()) {
      // 1. Mark span status as ERROR
      activeSpan.setStatus(StatusCode.ERROR);

      // 2. Official OpenTelemetry semantic conventions for exceptions
      if (ex != null) {
        activeSpan.recordException(ex);
        activeSpan.setAttribute("exception.type", ex.getClass().getName());
        String msg = ex.getMessage();
        if (msg != null && !msg.isBlank()) {
          activeSpan.setAttribute("exception.message", msg);
        }
      }

      // 3. Official machine-readable error code convention
      Object code = problem.getProperties() != null ? problem.getProperties().get("code") : null;
      if (code != null) {
        activeSpan.setAttribute("error.type", code.toString());
        activeSpan.setAttribute("error.code", code.toString());
      }

      // 4. Extended RFC 9457 attributes
      if (problem.getTitle() != null) {
        activeSpan.setAttribute("rfc9457.title", problem.getTitle());
      }
      activeSpan.setAttribute("rfc9457.status", (long) problem.getStatus());
    }
  }

  @Override
  public int order() {
    // Run after traceId and standard metadata enrichers
    return 200;
  }
}
