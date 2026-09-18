# Migration Guide

This guide helps you transition from vanilla Spring Boot's default error handling to the structured, RFC 9457-compliant `api-standard-spring-boot-starter`.

---

## 1. The Breaking Change: Response Payload

The most significant breaking change is the JSON structure returned to your clients.

**Old (Vanilla Spring Boot)**
```json
{
  "timestamp": "2026-05-18T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/resource"
}
```

**New (API Standard Starter / RFC 9457)**
```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/resource",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-05-18T10:00:00Z"
}
```

**Frontend Impact**: Any frontend application, mobile app, or API gateway that parses error messages must be updated to read the `detail` property instead of `message`, and `title` instead of `error`.

---

## 2. Migration Checklist

### Step 1: Remove Custom Global Handlers
If your project currently has a custom `@ControllerAdvice` or `@RestControllerAdvice` that catches `Exception.class` or `RuntimeException.class`, you should remove it or restrict its scope. Otherwise, it will compete with the starter's `GlobalExceptionHandler`.

### Step 2: Adopt `BusinessException`
Find your custom domain exceptions (e.g., `UserNotFoundException`, `InvalidPaymentException`).
Refactor them to either extend `BusinessException` directly or map them to the canonical `ErrorCode` values provided by the library.

### Step 3: Update Integration Tests
If you have integration tests (`@SpringBootTest` with `MockMvc`) that assert specific JSON paths (like `$.message`), you must update them:
*   Change `$.message` to `$.detail`.
*   Change `$.error` to `$.title`.
*   Change `$.path` to `$.instance`.

---

## 3. Gradual Rollout Strategy

If you have a large microservices architecture, do not attempt a "big bang" migration.

1.  **Phase 1: Dual-Parser on Frontend**. Update your Frontend HTTP interceptors (e.g., Axios, Fetch) to gracefully handle *both* formats. Check for the existence of `type` or `detail` to determine if the response is RFC 9457.
2.  **Phase 2: Opt-in Services**. Add the starter dependency to non-critical or newly created microservices first.
3.  **Phase 3: Core Services**. Migrate your core services and legacy monoliths.
4.  **Phase 4: Cleanup**. Remove the legacy parsers from the frontend.
