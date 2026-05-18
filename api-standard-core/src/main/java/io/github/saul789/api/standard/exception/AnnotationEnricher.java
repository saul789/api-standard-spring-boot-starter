package io.github.saul789.api.standard.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.ProblemDetail;
import org.springframework.util.ReflectionUtils;

/**
 * Enricher that scans the thrown Exception for fields annotated with
 * {@link ExposeToProblemDetail}
 * and adds their values to the ProblemDetail response.
 *
 * <p>
 * Implements the "Secure by Default" pattern. Only explicitly annotated fields
 * are exposed.
 */
public class AnnotationEnricher implements ProblemDetailEnricher {

  @Override
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale, Exception ex) {
    if (ex == null) {
      return;
    }

    ReflectionUtils.doWithFields(
        ex.getClass(),
        field -> {
          ExposeToProblemDetail annotation = field.getAnnotation(ExposeToProblemDetail.class);
          if (annotation != null) {
            ReflectionUtils.makeAccessible(field);
            Object value = field.get(ex);
            String propertyName = annotation.value().isEmpty() ? field.getName() : annotation.value();
            problem.setProperty(propertyName, value);
          }
        });
  }

  @Override
  public int order() {
    return 10; // Run before redaction (order 20) so fields added here can be redacted if
               // needed
  }
}
