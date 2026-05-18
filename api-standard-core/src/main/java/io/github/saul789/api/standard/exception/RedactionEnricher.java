package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.ProblemDetail;

/**
 * Enriches the ProblemDetail by masking sensitive field values in the error properties.
 *
 * <p>This enricher scans the {@code "errors"} extension property of the ProblemDetail for any
 * field whose name matches the configured sensitive fields, and replaces the value with the
 * configured mask (default: {@code "[REDACTED]"}).
 *
 * <p>The list of sensitive fields and the mask can be configured via:
 * <ul>
 *   <li>YML: {@code api.standard.redaction.fields} and {@code api.standard.redaction.mask}
 *   <li>Custom Bean: implement {@link RedactionPolicy} in your application
 * </ul>
 */
@SuppressWarnings("PMD.LooseCoupling") // Map<String,Object> is the API contract of ProblemDetail
public class RedactionEnricher implements ProblemDetailEnricher {

  private final RedactionPolicy policy;
  private final ApiStandardProperties properties;

  public RedactionEnricher(RedactionPolicy policy, ApiStandardProperties properties) {
    this.policy = policy;
    this.properties = properties;
  }

  @Override
  @SuppressWarnings("PMD.LooseCoupling")
  public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
    if (!properties.getRedaction().isEnabled()) {
      return;
    }

    Map<String, Object> props = problem.getProperties();
    if (props == null) {
      return;
    }

    Object errorsObj = props.get("errors");
    if (!(errorsObj instanceof List<?> errorList)) {
      return;
    }

    String mask = properties.getRedaction().getMask();
    List<Map<String, Object>> redactedErrors = buildRedactedErrors(errorList, mask);
    problem.setProperty("errors", redactedErrors);
  }

  @SuppressWarnings({"unchecked", "PMD.LooseCoupling"})
  private List<Map<String, Object>> buildRedactedErrors(List<?> errorList, String mask) {
    return errorList.stream()
        .filter(item -> item instanceof Map<?, ?>)
        .map(item -> redactEntry((Map<String, Object>) item, mask))
        .toList();
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private Map<String, Object> redactEntry(Map<String, Object> errorMap, String mask) {
    Object fieldObj = errorMap.get("field");
    if (!(fieldObj instanceof String fieldName) || !policy.shouldRedact(fieldName)) {
      return errorMap;
    }
    Map<String, Object> redacted = new HashMap<>(errorMap);
    redacted.put("rejectedValue", mask);
    return redacted;
  }

  @Override
  public int order() {
    return 20; // Run after standard enrichers and degraded mode enricher
  }
}
