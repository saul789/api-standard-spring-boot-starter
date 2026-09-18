# Frequently Asked Questions (FAQ)

This section covers common questions from different engineering roles regarding the architectural decisions and operational behavior of the `api-standard-spring-boot-starter`.

---

## Architecture & Design

### Q: Can I exclude specific controllers from the global error handler?
**A:** No. The `GlobalExceptionHandler` is annotated with `@RestControllerAdvice` without a `basePackages` restriction, meaning it applies globally to the entire Spring context. 
If you need custom error handling for a specific controller, you must define an `@ExceptionHandler` method *directly inside* that specific controller. Spring will give your local handler precedence over the global advice.

### Q: Why do we use RFC 9457 (`ProblemDetail`) instead of a custom JSON format?
**A:** To avoid "contract drift". In large enterprise ecosystems, every team tends to invent their own error JSON structure (`{"error":...}`, `{"status":...}`, `{"message":...}`). 
By adopting an IETF standard (RFC 9457), API consumers (like Frontend apps or external gateways) can write a single, universal error parser that works across all microservices.

---

## Performance & SRE

### Q: Does generating this JSON add latency to the API?
**A:** The JSON serialization (`ProblemDetailService`) and the enrichment pipeline have negligible overhead ($O(1)$ complexity). 
The actual performance cost during an error comes from the JVM generating the stack trace when the exception is initially `throw`n. Therefore, you should never use `BusinessException` for normal application control flow.

### Q: How do I correlate these error responses with my logs in Datadog, ELK, or Grafana?
**A:** The starter automatically runs the `TraceIdEnricher`. If a trace ID is present in the SLF4J Mapped Diagnostic Context (MDC), it will be injected into the error JSON under the `traceId` key. 
To complete the correlation, ensure your `logback-spring.xml` or logging configuration includes the MDC variable (e.g., `[%X{traceId}]`) so your logs match the JSON returned to the client.

---

## Security

### Q: How does the library prevent leaking PII in validation errors?
**A:** When a `MethodArgumentNotValidException` occurs, the starter exposes the field names that failed validation. To prevent leaking sensitive data (like a user submitting an invalid password), the library employs a `DefaultRedactionPolicy`. 
By default, it masks the values of fields named `password`, `token`, `secret`, `creditCard`, `cvv`, `ssn`, and `authorization`.

### Q: Can I change which fields are masked by the redaction policy?
**A:** Yes. You can override the default list in your `application.yml`:

```yaml
api:
  standard:
    redaction:
      enabled: true
      mask: "***"
      fields:
        - "password"
        - "myCustomSecretToken"
```
