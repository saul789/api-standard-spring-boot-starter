package io.github.saul789.api.standard.exception;

import java.net.URI;

/**
 * Interface that can be implemented by exceptions to provide a custom problem type URI (the {@code
 * type} field in RFC 9457 Problem Detail).
 *
 * <p>By default, the starter uses the configured {@code typeBaseUri} plus the kebab-case error
 * code. Implementing this interface allows full control over the URI on a per-exception basis.
 */
public interface ProblemTypeProvider {
  /**
   * Returns the custom problem type URI.
   *
   * @return the URI to use in the ProblemDetail response, or {@code null} to use the default.
   */
  URI getProblemType();
}
