# API Reference

This section provides a rigorous, production-grade API reference for the core components of the `api-standard-spring-boot-starter`. 

---

## 1. `BusinessException`

Signals a business-rule violation that should be communicated to the caller with a specific HTTP status code and `ErrorCode`.

### Complete Signature

```java
public class BusinessException extends ProblemException {
  public BusinessException(ErrorCode code, String message, HttpStatus status);
  public BusinessException(String message, HttpStatus status);
  public BusinessException(String message, HttpStatus status, String customUrl);
  public BusinessException(ErrorCode code, String message, HttpStatus status, String type);
  public BusinessException(ErrorCode code, String message, HttpStatus status, URI type);
  
  public static Builder builder(String message);
}
```

### Parameters (Main Constructor)

| Name | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `code` | `ErrorCode` | Yes | Derived from status | The machine-readable canonical error code. |
| `message` | `String` | Yes | - | User-friendly detail message or i18n translation key. |
| `status` | `HttpStatus` | Yes | `BAD_REQUEST` | The HTTP status to return to the client. |

### Returns & Contracts
*   **Shape**: Extends `RuntimeException`.
*   **Mutability**: Immutable once instantiated.
*   **Lifecycle**: Ephemeral. Created at the moment of failure, caught by `GlobalExceptionHandler`, and immediately garbage collected after the response is rendered.
*   **Ownership**: Owned by the domain/application layer throwing it.

### Throws
As an exception itself, it is meant to be thrown. 
*   **Retryability**: Generally **not retryable** without modifying the request (since it represents a business rule violation, e.g., 409 Conflict or 422 Validation Error).
*   **Recoverability**: Handled automatically by the starter's global handler.

### Performance
*   **Complejidad (Complexity)**: $O(1)$.
*   **Allocations**: Minimal, but involves standard JVM stack trace generation which is relatively heavy.
*   **Impacto Runtime**: Designed for exceptional flows. Should not be used for normal control flow to avoid stack trace generation overhead.

### Security
*   **Validación**: Requires non-null message if using the Builder.
*   **Unsafe Patterns**: Do **not** pass raw user input or PII directly into the `message` field unless it's a translation key, as this message is exposed directly to the HTTP client.

### Edge Cases
*   **Nullability**: If `code` is passed as null in the constructor, it resolves to a default based on the HTTP status.
*   **Translation Fallback**: If the `message` is intended to be an i18n key but the key is not found in `messages.properties`, the key itself is returned to the user.

### Examples

**Basic Example**
```java
if (user.isBanned()) {
    throw new BusinessException(ErrorCode.FORBIDDEN, "User is currently banned", HttpStatus.FORBIDDEN);
}
```

**Advanced Example (Builder)**
```java
throw BusinessException.builder("error.payment.insufficient_funds")
    .status(HttpStatus.PAYMENT_REQUIRED)
    .code(ErrorCode.VALIDATION_ERROR)
    .type(URI.create("https://docs.mycompany.com/errors/funds"))
    .build();
```

---

## 2. `ProblemDetailEnricher`

The primary extension hook for adding custom metadata to all RFC 9457 error responses.

### Complete Signature

```java
public interface ProblemDetailEnricher {
  default void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale);
  default void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale, Exception ex);
  default int order();
}
```

### Parameters

| Name | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `problem` | `ProblemDetail` | Yes | - | The mutable problem detail object to enrich. |
| `request` | `HttpServletRequest` | Yes | - | The current HTTP request context. |
| `locale` | `Locale` | Yes | - | The resolved locale for the request. |
| `ex` | `Exception` | No | null | The original exception that triggered the error. |

### Returns & Contracts
*   **Mutabilidad**: Implementations **must** mutate the injected `ProblemDetail` object (typically using `problem.setProperty()`).
*   **Lifecycle**: Beans implementing this interface are collected at application startup. The `enrich` method is called synchronously on every error request.
*   **Order**: Governed by the `order()` method (lower values execute first).

### Performance
*   **Bottlenecks**: Because this runs synchronously in the Servlet thread during error handling, implementations **must not** perform blocking I/O (e.g., database calls or HTTP requests).
*   **Caching**: If enrichment requires reference data, it must be cached aggressively in memory.

### Seguridad
*   **Sanitización**: Any data extracted from `HttpServletRequest` (e.g., headers) and injected into the response must be sanitized to prevent Log Forging or XSS (though Spring's JSON serializer handles basic escaping).

### Edge Cases
*   **Stale State**: If extracting data from `SecurityContextHolder`, be aware that the context might be partially cleared or empty if the error occurred very early in the filter chain.
*   **Nullability**: `ex` can be null in some synthetic error scenarios.

### Examples

**Built-in Trace ID Enrichment (from library source)**
```java
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
```

---

## 3. `ErrorCode`

Canonical enumeration of supported error identifiers.

### Complete Signature

```java
public enum ErrorCode {
  INTERNAL_ERROR, BAD_REQUEST, VALIDATION_ERROR, GONE, UNAUTHORIZED, 
  FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, 
  CONFLICT, TOO_MANY_REQUESTS, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT;

  public static ErrorCode fromStatus(int status);
  public String toKebabCase();
}
```

### Returns & Contracts
*   **Shape**: Java `enum`.
*   **Mutabilidad**: Strictly immutable.
*   **Contratos**: Designed to provide a finite, machine-readable vocabulary for error classification across the organization.

### Edge Cases
*   **Status Fallback (`fromStatus`)**: If an unrecognized HTTP status is passed (e.g., `418`), it falls back to `BAD_REQUEST` if the status is 4xx, and `INTERNAL_ERROR` otherwise.

---

## 4. `ApiStandardProperties`

The `@ConfigurationProperties` class that governs the library's behavior.

### Complete Signature

```java
@ConfigurationProperties(prefix = "api.standard")
public class ApiStandardProperties {
    public Errors getErrors();
    public DegradedMode getDegradedMode();
    public Redaction getRedaction();
}
```

*(For a full list of YAML/Properties parameters, refer to the [Installation](installation.md) guide).*

### Lifecycle & Mutability
*   **Lifecycle**: Instantiated once during Spring Boot context initialization.
*   **Mutabilidad**: Fields are mutable via setters (required by Spring Boot), but modifying them programmatically post-startup is an **anti-pattern** and may lead to inconsistent behavior across threads.
