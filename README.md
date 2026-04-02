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
        // Option 1: Full control with explicit ErrorCode
        throw new BusinessException(ErrorCode.BAD_REQUEST, "error.user_exists", HttpStatus.BAD_REQUEST);

        // Option 2: Simple usage (ErrorCode resolved from status 400 -> BAD_REQUEST)
        // throw new BusinessException("Email already exists", HttpStatus.BAD_REQUEST);
    }
}
```

**Client Error Response (HTTP 400):**
```json
{
    "type": "urn:problem-type:bad-request",
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
    "type": "urn:problem-type:conflict",
    "title": "Conflict",
    "status": 409,
    "detail": "User with email john@doe.com already exists",
    "code": "CONFLICT",
    "timestamp": "...",
    "traceId": "..."
}
```

### 4. Custom documentation URLs

You can override the default URN by providing a custom documentation URL:

```java
// Option 1: Using BusinessException / ProblemException constructor
throw new BusinessException("No funds", HttpStatus.BAD_REQUEST, "https://docs.myapi.com/errors/insufficient-funds");

// Option 2: Using @ProblemType annotation on your custom exception
@ProblemType("https://docs.myapi.com/errors/custom-error")
public class MySpecificException extends RuntimeException { ... }

// Option 3: Global configuration in application.yml
api:
  standard:
    errors:
      type-overrides:
        BAD_REQUEST: "https://docs.myapi.com/errors/general-bad-request"
```

### 5. ResponseStatusException

Direct use of `ResponseStatusException` is also fully supported and localized.

### 6. Validation Errors (`@Valid` / `@Validated`)

If you use annotation-based validation for your request payloads, the library automatically formats the field errors in a standardized way:
```json
{
    "type": "urn:problem-type:validation-error",
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

### 7. OpenFeign Integration

The library automatically handles exceptions thrown by Feign clients. If a downstream service returns a `ProblemDetail` or a generic error, the starter intercepts it and translates it to the standardized format, preserving the HTTP status and providing a `BAD_GATEWAY` or `GATEWAY_TIMEOUT` code if appropriate.

### 8. i18n Support & Message Personalization

The library intelligently resolves the `detail` and `title` fields using Spring's `MessageSource` and the current `Locale` (automatically extracted from the `Accept-Language` header).

#### How it works:
1. **Title Translation:** The library always attempts to translate the title using the key `error.<CODE_NAME>`. If no translation is found, it falls back to the standard HTTP Reason Phrase.
2. **Detail Personalization:**
   - **Using a Key:** If the message passed to `BusinessException` is a key in your `messages.properties` (e.g., `error.low_balance`), it will be translated to the user's language.
   - **Using Fixed Text:** If the message is NOT a key (e.g., "User 'saul' not found"), the library detects this and returns the text exactly as provided. This allows for dynamic, personalized error messages.

**Example `messages_es.properties`:**
```properties
error.BAD_REQUEST=Petición Incorrecta
error.business.default=Se ha violado una regla de negocio.
```

**Scenario: Throwing a key**
```java
throw new BusinessException(ErrorCode.BAD_REQUEST, "error.business.default", HttpStatus.BAD_REQUEST);
// Result (Accept-Language: es) -> detail: "Se ha violado una regla de negocio."
```

**Scenario: Throwing custom text**
```java
throw new BusinessException(ErrorCode.BAD_REQUEST, "El usuario Saul ya tiene un plan activo", HttpStatus.BAD_REQUEST);
// Result -> detail: "El usuario Saul ya tiene un plan activo" (No translation attempted as it's not a key)
```

### 9. Trace Context & Logging

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

---

### 10. UserController Example (Audited & Standardized)

To see everything in action, the `sample-project` includes a `UserController` that demonstrates the full power of the library:

**Model Validation + Business Logic:**
```java
@PostMapping
public String createUser(@Valid @RequestBody UserRequest request) {
    if ("test@example.com".equals(request.email())) {
        throw new BusinessException(
            ErrorCode.CONFLICT, 
            "error.user.already_exists", 
            HttpStatus.CONFLICT,
            "https://api.saul.dev/docs/errors/user-limits"
        );
    }
    return "User created!";
}
```

**Scenario 1: Validation Error (Empty Name)**
Returns HTTP 400 with `code: VALIDATION_ERROR` and a list of field-specific messages from your `messages.properties`.

**Scenario 2: Business Logic Error (Duplicate Email)**
Returns HTTP 409 with `code: CONFLICT`, the translated message from `error.user.already_exists`, and the custom documentation URL in the `type` field.
