package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;

/**
 * Default implementation of {@link RedactionPolicy} that reads sensitive field names from
 * {@code api.standard.redaction.fields} in the application configuration.
 */
public class DefaultRedactionPolicy implements RedactionPolicy {

  private final ApiStandardProperties properties;

  public DefaultRedactionPolicy(ApiStandardProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean shouldRedact(String fieldName) {
    return fieldName != null && properties.getRedaction().getFields().stream()
        .anyMatch(sensitive -> sensitive.equalsIgnoreCase(fieldName));
  }
}
