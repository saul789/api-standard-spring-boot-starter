package io.github.saul789.api.standard.exception;

/**
 * Strategy interface that determines which fields should be redacted in error responses.
 *
 * <p>Implement this interface and register it as a Spring Bean in your application to fully
 * customize the redaction behavior. If no bean is provided, the default implementation
 * ({@link DefaultRedactionPolicy}) reads the list from {@code api.standard.redaction.fields}.
 *
 * <p>Example:
 * <pre>{@code
 * @Bean
 * public RedactionPolicy myPolicy() {
 *     return fieldName -> fieldName.endsWith("Token") || fieldName.contains("secret");
 * }
 * }</pre>
 */
@FunctionalInterface
public interface RedactionPolicy {

  /**
   * Determines if the given field name contains sensitive data and should be masked.
   *
   * @param fieldName the name of the field (e.g., "password", "creditCard")
   * @return true if the field value should be replaced with the configured mask
   */
  boolean shouldRedact(String fieldName);
}
