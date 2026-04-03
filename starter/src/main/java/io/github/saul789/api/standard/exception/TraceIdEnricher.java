package io.github.saul789.api.standard.exception;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Enricher for traceId metadata.
 */
@Component
public class TraceIdEnricher implements ProblemDetailEnricher {
    @Override
    public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    @Override
    public int order() {
        return 100;
    }
}
