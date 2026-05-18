<div align="center">
  <h1>🚀 api-standard-spring-boot-starter</h1>
  <p><b>Bring organization-wide API consistency to every Spring Boot service.</b></p>
  <p><i>Turn Spring Boot into a governed API platform with one dependency. Standardize responses, errors, tracing, and OpenAPI documentation automatically.</i></p>
  
  [![Spring Boot 4.0.6](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
  [![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
  [![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/saul789/api-standard-spring-boot-starter/releases)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Docs](https://img.shields.io/badge/docs-Redoc-blue.svg)](https://saul789.github.io/api-standard-spring-boot-starter/)
  
  <br/>
</div>

A plug-and-play Spring Boot starter that standardizes API responses, implements **RFC 9457** (Problem Details for HTTP APIs), and provides distributed tracing via Trace Context propagation.

  <p align="center">
    <!-- Replace this with a real screenshot of Swagger UI showing Success Response, RFC 9457 Error and traceId
    <img src="./docs/assets/hero-swagger-preview.png" width="900" alt="Swagger UI showing Success Response, RFC 9457 Error and traceId"/>
    -->
  </p>
</div>

---

## 🚫 Stop Copy-Pasting API Infrastructure

Most organizations eventually create:
- Duplicated `GlobalExceptionHandler`s in every project
- Inconsistent `ApiResponse` wrappers
- Incompatible error formats across teams
- Fragmented Swagger contracts

**This starter centralizes those cross-cutting concerns into a single reusable standard.** Stop reinventing the wheel and eliminate boilerplate technical debt.

## 🏢 Ideal For
- Multi-team microservice environments
- Internal Platform Teams (Platform Engineering)
- Enterprise API Governance Initiatives
- Organizations adopting RFC 9457 (Problem Details)
- Teams standardizing observability and tracing

---

## ⚡ Zero-Configuration Philosophy

- **No annotations** on your controllers.
- **No inheritance** of base classes.
- **No custom exception handlers** to maintain.
- **No duplicated response wrappers**.

Add the dependency and keep building your API. We handle the rest.

---

## 🏗️ Architecture Flow

```mermaid
graph TD
    A[Client Request] --> B[TraceContextFilter]
    B --> C[Spring Controller]
    C -- "Throws Exception" --> D[GlobalExceptionHandler]
    D --> E[ProblemDetailService]
    E -- "1. Map ErrorCode" --> F[i18n Messages]
    E -- "2. Enrich metadata" --> G[Enricher SPI]
    F --> H[ProblemDetail JSON]
    G --> H
    H --> I[Client Response RFC 9457]
    
    subgraph "api-standard-starter"
    D
    E
    F
    G
    end
```

## 📦 Modules

The project is divided into a multi-module architecture for better separation of concerns:
- **`api-standard-core`**: Core contracts, base exceptions (`BusinessException`, `ProblemException`), and models. No Spring Web dependencies.
- **`api-standard-web`**: Spring MVC adapters, `GlobalExceptionHandler`, filters, and optional integrations (Feign, Resilience4j).
- **`api-standard-security`**: Native Spring Security exception handling mapped to RFC 9457 Problem Details.
- **`api-standard-starter`**: The auto-configuration module that brings everything together as a plug-and-play dependency.

## 🚀 Quick Start

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.saul789</groupId>
    <artifactId>api-standard-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

### 2. Create a normal controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User get(@PathVariable String id) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "User not found", HttpStatus.NOT_FOUND);
    }
}
```

### 3. Automatic API Governance Enabled

Without writing any boilerplate, your API now automatically returns standardized responses, RFC 9457 errors, traceIds, auto-generated OpenAPI schemas, and translated messages.

---

## 📦 The Standard Contract

By simply returning objects or throwing exceptions, the starter enforces a rigorous contract globally:

### ✅ Success Response (200 OK)
```json
{
  "success": true,
  "timestamp": "2026-05-10T18:34:45Z",
  "data": { 
      "id": "123", 
      "name": "Jane Doe" 
  }
}
```

### ❌ Error Response (RFC 9457 Compliant)
```json
{
  "type": "https://api.yourdomain.com/errors/not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User with ID 123 does not exist",
  "instance": "/api/users/123",
  "code": "NOT_FOUND",
  "timestamp": "2026-05-10T18:34:46Z",
  "traceId": "5f9b3b8c-1234-4a56-b789-abcdef123456"
}
```

### 📉 Degraded Mode (New in V2)
When the system is under high CPU load, the starter can run in degraded mode to protect the service and inform clients:
- **JSON Flag**: Adds `"degraded": true` to the error response.
- **HTTP Header**: Adds `X-API-Degraded: true` to the response headers.

Configuration properties in `application.yml`:
```yaml
api:
  standard:
    degraded-mode:
      enabled: true
      cpu-threshold: 80.0
      show-in-json: true
      show-in-header: true
```

> 💡 **Bonus: Transparent MDC Logging**
> Every request gets a `traceId` injected into SLF4J MDC instantly. Watch your logs become 10x easier to debug:
> `INFO [traceId: 5f9b3b8c-1234...] c.s.UserController: Fetching user 123`

---

## ✨ The Organizational Savings

| Capability | Traditional Setup | This Starter |
|---|---|:---:|
| **RFC 9457 Compliance** | Manual construction | ✅ Automatic |
| **OpenAPI Response Wrapping** | Manual annotations on every method | ✅ Automatic |
| **TraceId Propagation** | Custom Servlet filters | ✅ Built-in |
| **Error Translation (i18n)** | Custom boilerplate | ✅ Built-in |
| **Organization-wide Consistency**| Hard to enforce | ✅ Automatic |

---

## 🔍 Before vs After

**Before (Manual & Boilerplate):**
```diff
- @RestControllerAdvice
- public class GlobalExceptionHandler { ... }
-
- public class ApiResponse<T> { ... }
-
- @Component
- public class TraceFilter extends OncePerRequestFilter { ... }
-
- @PostMapping
- @ApiResponses({
-     @ApiResponse(responseCode = "200", description = "Success"),
-     @ApiResponse(responseCode = "400", description = "Bad Request")
- })
- public ResponseEntity<ApiResponse<User>> createUser(@RequestBody UserRequest request) { ... }
```

**After (Using this Starter):**
```diff
+ @PostMapping
+ @ResponseStatus(HttpStatus.CREATED)
+ public User createUser(@Valid @RequestBody UserRequest request) {
+     return userService.create(request);
+ }
```

---

## 🛡️ Enterprise-Grade Foundations

Built for large-scale microservice platforms from day one:
- ✅ **Stateless & thread-safe**
- ✅ **Native Spring Boot Auto-configuration**
- ✅ **RFC 9457** strictly compliant
- ✅ **OpenAPI v3** native auto-wrapping
- ✅ **Works with OpenFeign & Resilience4j**

## ⚙️ How It Works (For the Skeptics)

The starter integrates deeply with Spring Boot auto-configuration:
- `ResponseBodyAdvice` standardizes responses automatically.
- `@RestControllerAdvice` maps exceptions into RFC 9457 format.
- Servlet filters propagate `traceId` into the SLF4J MDC.
- OpenAPI schemas are rewritten dynamically at runtime.
- `MessageSource` enables transparent i18n translation.

*No annotations required.*

---

## 🚀 V2 Features (Error Intelligence Platform)

This starter has evolved into a comprehensive **Error Intelligence Platform**:

### 🔒 Redaction Engine (Secure by Default)
Automatically masks sensitive fields (like `password`, `token`, `secret`) in your validation errors and API responses before they leave your system. Configurable via `application.yml`:
```yaml
api:
  standard:
    redaction:
      enabled: true
      fields: "password, token, secret, credit_card"
      mask: "***"
```

### 🎯 Opt-in Field Exposure
Exceptions hide internal states by default. Use `@ExposeToProblemDetail` to selectively expose safe diagnostic fields directly into the RFC 9457 `ProblemDetail`.

```java
public class InsufficientFundsException extends BusinessException {
    @ExposeToProblemDetail("balance") // Optional alias
    private final BigDecimal currentBalance;
    // ...
}
```

### 📊 Automated Metrics (Micrometer)
Zero-code observability. If `micrometer-core` is on your classpath, the starter automatically registers an `api.errors.total` counter tagged by `code`, `status`, `method`, and `uri` (using safe Spring MVC patterns to prevent cardinality explosion).

### 🩺 Diagnostic Actuator Endpoint
Expose the internal state of the Error Intelligence platform. Access `/actuator/api-errors` to see live configuration of Degraded Mode, Redaction Engine, and current thresholds.

---

## 🛠️ Architecture Overview

<details>
<summary><b>Click to view architecture diagram</b></summary>

```mermaid
sequenceDiagram
    participant Client
    participant TraceFilter
    participant Controller
    participant ExceptionHandler
    
    Client->>TraceFilter: GET /api/users
    activate TraceFilter
    TraceFilter->>TraceFilter: Generate UUID (traceId)
    TraceFilter->>Controller: MDC Injected Request
    
    activate Controller
    Controller-->>ExceptionHandler: throw BusinessException
    deactivate Controller
    
    activate ExceptionHandler
    ExceptionHandler->>ExceptionHandler: Map to RFC 9457 & i18n
    ExceptionHandler-->>TraceFilter: ProblemDetail JSON
    deactivate ExceptionHandler
    
    TraceFilter-->>Client: HTTP 400 + JSON
    deactivate TraceFilter
```
</details>

---

## 🤝 Contributing & Testing

- **API Client Collection:** Available in `sample-project/postman/api-standard-spring-boot-starter.postman_collection.json`. *(Importable in Postman, Insomnia, Bruno, Hoppscotch, etc.)*
- **Interactive Documentation:** Check the GitHub Pages branch for the Redocly auto-generated site.

## 📄 License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

---
**Do you find this useful?** Give us a ⭐ on GitHub to support the project!