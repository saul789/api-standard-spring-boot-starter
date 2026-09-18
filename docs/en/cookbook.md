# Cookbook: Practical Recipes 👨‍🍳

All recipes in this cookbook are taken directly from the [sample-project](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project) included in this repository. Every code example has been verified against the real codebase.

---

## The Core Concept: `ProblemDetailEnricher`

Instead of modifying the core error handler, the library provides a simple interface called `ProblemDetailEnricher`. 

Spring Boot automatically detects any class in your project that implements this interface. When an error occurs, the library passes the `ProblemDetail` object to your class, allowing you to add custom fields (properties) before the JSON is sent to the client.

---

## Recipe 1: Injecting a Static Application Name

**Scenario**: You have multiple microservices using this library, and you want every error response to explicitly state which microservice generated the error.

### The Code

Create a new class in your configuration package:

```java
package com.example.config;

import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class AppNameEnricher implements ProblemDetailEnricher {

    @Override
    public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
        // Add a static string identifying this microservice
        problem.setProperty("service", "billing-api");
    }

    @Override
    public int order() {
        // 100 ensures it runs after the built-in enrichers.
        return 100; 
    }
}
```

### The Result

When an error happens, your JSON will now automatically include the `"service"` field:

```json
{
  "type": "urn:problem-type:bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid parameters.",
  "traceId": "9b1deb4d-3b7d-4bad",
  "service": "billing-api" 
}
```

---

## Recipe 2: Throwing Business Exceptions with ErrorCode

**Scenario**: Your domain logic detects a business rule violation (e.g., duplicate resource, insufficient permissions) and you need to communicate it cleanly to the API client.

*Source: [ExceptionDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ExceptionDemoController.java)*

### The Code

```java
// Option A: Using the canonical ErrorCode enum
@GetMapping("/business-logic")
public void throwBusiness() {
    throw new BusinessException(
        ErrorCode.BAD_REQUEST, "Business rule violated.", HttpStatus.BAD_REQUEST);
}

// Option B: Using ProblemException with automatic URN generation
@GetMapping("/problem-internal")
public void throwInternal() {
    // Automatically generates: urn:problem-type:too-many-requests
    throw new ProblemException("Rate limit reached.", HttpStatus.TOO_MANY_REQUESTS);
}

// Option C: Using a custom documentation URL instead of the auto-generated URN
@GetMapping("/problem-external")
public void throwExternal() {
    throw new ProblemException(
        "Validation error.",
        HttpStatus.BAD_REQUEST,
        "https://docs.saul.dev/errors/validation-guide");
}
```

### The Result

```json
{
  "type": "urn:problem-type:bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Business rule violated.",
  "instance": "/api/demo/exceptions/business-logic",
  "code": "BAD_REQUEST",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "timestamp": "2026-05-18T21:34:00Z"
}
```

---

## Recipe 3: i18n — Automatic Message Translation

**Scenario**: Your API supports multiple languages. You want error messages to automatically translate based on the client's `Accept-Language` header, without changing any controller code.

*Source: [ExceptionDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ExceptionDemoController.java) + [messages.properties](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/resources/messages.properties)*

### The Code

**Step 1**: Define your keys in `messages.properties` and `messages_es.properties`:

```properties
# messages.properties (English - default)
sample.error.limit_reached=The project limit has been reached for user {0}.
error.user.already_exists=User with this email address is already registered.
```

```properties
# messages_es.properties (Spanish)
sample.error.limit_reached=Se ha alcanzado el límite del proyecto para el usuario {0}.
error.user.already_exists=El usuario con este correo ya se encuentra registrado.
```

**Step 2**: Throw the exception using the i18n key as the message:

```java
// The library will resolve "sample.error.limit_reached" from messages.properties
@GetMapping("/client-key")
public void throwClientKey() {
    throw new BusinessException(
        ErrorCode.FORBIDDEN, "sample.error.limit_reached", HttpStatus.FORBIDDEN);
}
```

### The Result

With `Accept-Language: es`:

```json
{
  "type": "urn:problem-type:forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Se ha alcanzado el límite del proyecto para el usuario {0}.",
  "code": "FORBIDDEN",
  "traceId": "..."
}
```

> **Note**: If the key is not found in `messages.properties`, the raw string is returned as-is to the client, allowing fully custom messages without properties files.

---

## Recipe 4: Bean Validation with Automatic Error Details

**Scenario**: You want structured, detailed validation error responses when a client sends invalid JSON or query parameters.

*Source: [ValidationDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ValidationDemoController.java) + [UserDto.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/dto/UserDto.java)*

### The Code

**DTO with JSR-303 annotations:**
```java
public class UserDto {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotNull(message = "Age is required")
    private Integer age;
}
```

**Controller using `@Valid`:**
```java
@PostMapping("/body")
public UserDto testBodyValidation(@Valid @RequestBody UserDto dto) {
    return dto;
}
```

### The Result

Send `POST /api/demo/validation/body` with `{"username": "", "age": null}`:

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "username: Username cannot be blank; age: Age is required",
  "code": "VALIDATION_ERROR",
  "traceId": "..."
}
```

> **Important**: The Redaction Engine will automatically mask values for fields matching configured sensitive names (e.g., `password`, `token`, `secret`).

---

## Recipe 5: Feign Client Error Propagation

**Scenario**: Microservice A calls Microservice B via Feign. B fails. You need the error to propagate cleanly to the original caller without losing context.

*Source: [FeignDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/FeignDemoController.java)*

### How It Works

The library automatically detects `FeignException` on the classpath and intercepts it:

- **Client errors (4xx)**: Propagated as-is (e.g., 404 → 404).
- **Server errors (5xx) or unknown**: Transformed into `502 Bad Gateway` to avoid exposing internal details.

### The Code

```java
// This simulates an upstream 503 failure → the library returns 502 Bad Gateway
@GetMapping("/upstream-failure")
public void throwFeignServiceUnavailable() {
    throw new feign.FeignException.ServiceUnavailable(
        "Upstream service down",
        feign.Request.create(
            feign.Request.HttpMethod.GET, "/external/api",
            java.util.Collections.emptyMap(), null, null, null),
        null, null);
}

// This simulates an upstream 404 → the library propagates it as 404
@GetMapping("/upstream-notfound")
public void throwFeignNotFound() {
    throw new feign.FeignException.NotFound(
        "Resource not found in remote service",
        feign.Request.create(
            feign.Request.HttpMethod.GET, "/remote-resource",
            java.util.Collections.emptyMap(), null, null, null),
        null, null);
}
```

---

## Recipe 6: Resilience4j Exception Handling

**Scenario**: Your API uses Resilience4j for circuit breakers, rate limiters, or bulkheads. When these mechanisms trip, you need clean RFC 9457 responses instead of raw stack traces.

*Source: [ResilienceDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ResilienceDemoController.java)*

### How It Works

The `GlobalExceptionHandler` natively detects and maps Resilience4j exceptions:

| Exception | HTTP Status | Error Code |
| :--- | :--- | :--- |
| `CallNotPermittedException` | `503 Service Unavailable` | `SERVICE_UNAVAILABLE` |
| `RequestNotPermitted` | `429 Too Many Requests` | `TOO_MANY_REQUESTS` |
| `BulkheadFullException` | `429 Too Many Requests` | `TOO_MANY_REQUESTS` |
| `TimeoutException` | `504 Gateway Timeout` | `GATEWAY_TIMEOUT` |

### The Code

```java
@GetMapping("/circuit-breaker")
public void circuitBreaker() {
    CircuitBreaker cb = CircuitBreaker.ofDefaults("demo-circuit");
    throw CallNotPermittedException.createCallNotPermittedException(cb);
}

@GetMapping("/rate-limiter")
public void rateLimiter() {
    RateLimiter limit = RateLimiter.of("demo-rate",
        RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .build());
    throw RequestNotPermitted.createRequestNotPermitted(limit);
}
```

### The Result

```json
{
  "type": "urn:problem-type:too-many-requests",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "RequestNotPermitted: ...",
  "code": "TOO_MANY_REQUESTS",
  "traceId": "..."
}
```

---

## Recipe 7: Custom Exception with `@ResponseStatus`

**Scenario**: You have legacy domain exceptions annotated with `@ResponseStatus` and want the library to handle them automatically without modifications.

*Source: [CustomConflictException.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/exception/CustomConflictException.java)*

### The Code

```java
@ResponseStatus(HttpStatus.CONFLICT)
public class CustomConflictException extends RuntimeException {
    public CustomConflictException(String message) {
        super(message);
    }
}
```

Usage in a controller:

```java
@GetMapping("/annotated")
public void throwAnnotated() {
    throw new CustomConflictException("This resource is currently being edited.");
}
```

### The Result

```json
{
  "type": "urn:problem-type:conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "This resource is currently being edited.",
  "code": "CONFLICT",
  "traceId": "..."
}
```

> The library reads the `@ResponseStatus` annotation from your exception class and uses it to set the HTTP status and derive the `type` URN.
