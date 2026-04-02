package io.github.saul789.api.standard.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;

/**
 * Enricher for timestamp and instance metadata.
 */
@Component
public class StandardMetadataEnricher implements ProblemDetailEnricher {
    @Override
    public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
        problem.setProperty("timestamp", Instant.now());
        try {
            problem.setInstance(URI.create(request.getRequestURI()));
        } catch (Exception _) {
            // Null or invalid request URI fallback handled at higher level if needed
        }
    }

    @Override
    public int order() {
        return 0; // Run early
    }
}
