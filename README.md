# Spring Boot Starter API Standard

A comprehensive, "plug and play" Spring Boot starter library designed to standardize API responses, implement global exception handling using RFC 9457 (`ProblemDetail`), and provide deep observability via structured logging and Trace Context propagation.

## 🚀 Features

- **Standardized API Responses:** Automatically wraps all controller responses in a consistent JSON envelope, maintaining a clean and uniform API contract.
- **Robust Error Handling (RFC 9457):** Out-of-the-box global exception handling implementing Spring Boot 3+ `ProblemDetail` specification. Includes built-in support for validation errors, business exceptions, and generic server errors.
- **Intelligent Exception Mapping:** Automatically respects `@ResponseStatus` annotations and `ResponseStatusException`. Resolves appropriate machine-readable `ErrorCode` based on HTTP status (e.g., 404 -> `NOT_FOUND`).
- **OpenFeign Integration:** Seamlessly handles Feign client exceptions, propagating error details correctly across microservices.
- **Trace Context Propagation:** Automatically generates and propagates a `traceId` for every incoming request. Intercepts logs using MDC (Mapped Diagnostic Context) to facilitate distributed tracing.
- **i18n Support:** Fully compatible with Spring's `MessageSource` for localizing error messages.

## 📦 Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.saul789</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

*(Note: Ensure you have your Nexus/Maven repository configured correctly to fetch this artifact once published)*.

## 🛠️ How it Works

Once the dependency is added, **no additional configuration is required**. The starter will auto-configure several beans to intercept and standardize traffic.

### 1. Standardized API Responses (`ApiResponseAdvice`)

You simply return your DTOs or primitive values from your controllers, and the library automatically wraps them in a standard structure.

**Your Controller:**
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return new User(id, "John Doe");
    }
}
```

**What the Client Receives:**
```json
{
  "success": true,
  "data": {
    "id": "123",
    "name": "John Doe"
  },
  "timestamp": "2023-10-25T10:00:00Z"
}
```
*(The wrapper schema may vary slightly based on your `ApiResponse` model)*

### 2. Global Exception Handling (`GlobalExceptionHandler`)

The library gracefully intercepts exceptions and translates them into standard RFC 9457 Problem Details.

**Throwing a Business Exception:**
```java
import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;

@Service
public class UserService {
    public void validateUser(String email) {
        // ...
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Email already exists");
    }
}
```

**Client Error Response (HTTP 400):**
```json
 {
     "type": "about:blank",
     "title": "Bad Request",
     "status": 400,
     "detail": "Email already exists",
     "instance": "/api/users",
     "code": "BAD_REQUEST",
     "timestamp": "2023-10-25T10:05:00Z",
     "traceId": "5f9b3b8c-1234-4a56-b789-abcdef123456"
 }
 ```

_Notice the inclusion of the `traceId` which helps with debugging and log tracing!_

### 3. Annotation-based Exceptions (`@ResponseStatus`)

The library is intelligent enough to respect your own custom exceptions decorated with `@ResponseStatus`. It will even map the correct `code` based on the status provided.

**Custom Exception:**
```java
@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
```

**Resulting JSON:**
```json
{
    "title": "Conflict",
    "status": 409,
    "detail": "User with email john@doe.com already exists",
    "code": "BAD_REQUEST",
    "timestamp": "...",
    "traceId": "..."
}
```

### 4. ResponseStatusException

Direct use of `ResponseStatusException` is also fully supported and localized.

### 5. Validation Errors (`@Valid` / `@Validated`)

If you use annotation-based validation for your request payloads, the library automatically formats the field errors in a standardized way:
```json
{
    "type": "about:blank",
    "title": "Validation Error",
    "status": 400,
    "detail": "error.validation.body",
    "instance": "/api/users",
    "code": "VALIDATION_ERROR",
    "errors": [
        {
            "field": "email",
            "message": "must be a well-formed email address"
        }
    ],
    "timestamp": "2023-10-25T10:10:00Z",
    "traceId": "6a8c4d9e-..."
}
```

### 6. Trace Context & Logging

The included filters (`TraceContextFilter` and `RequestLoggingFilter`) automatically:
1. Extract an incoming `traceId` header or generate a new UUID.
2. Inject it into the SLF4J MDC (`MDC.put("traceId", ...)`).
3. Log incoming requests processing and completion transparently.

Because the library integrates with MDC out of the box, all your application logs will automatically share the same `traceId`, which is essential for distributed monitoring tools like ELK, Splunk, Datadog or Zipkin.

## ⚙️ Requirements

- **Java:** 25 or higher
- **Spring Boot:** 4.0.x or higher

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a Pull Request if you find any bugs or want to propose new features.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📮 Postman Collection

Para facilitar las pruebas, se incluye una colección de Postman en:
`sample-project/postman/spring-boot-starter-api-standard.postman_collection.json`
