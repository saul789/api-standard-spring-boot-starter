package io.github.saul789.api.standard.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.ProblemDetail;

/**
 * Interface for components that enrich a ProblemDetail with additional metadata. Part of the SOLID
 * refactoring (Open/Closed principle).
 */
public interface ProblemDetailEnricher {
  /**
   * Enriches the given ProblemDetail with specific metadata.
   *
   * @param problem the problem detail to enrich
   * @param request the current HTTP request
   * @param locale the locale for translations
   */
  void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale);

  /**
   * Optional order of execution.
   *
   * @return priority order
   */
  default int order() {
    return 0;
  }
}
