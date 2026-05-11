<div align="center">
  <h1>🚀 Spring Boot Starter: API Standard</h1>
  <p><b>Enterprise-grade API Standardization, RFC 9457 Errors, Trace Context & OpenAPI Auto-Integration</b></p>
  
  [![CI](https://github.com/saul789/spring-boot-starter-api-standard/actions/workflows/publish.yml/badge.svg)](https://github.com/saul789/spring-boot-starter-api-standard/actions/workflows/publish.yml)
  [![Version](https://img.shields.io/badge/version-1.2.0-blue.svg)](https://github.com/saul789/spring-boot-starter-api-standard/releases)
  [![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
  [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
  <br/>
  <i>Stop writing boilerplate for API responses, error handling, and tracing in every Spring Boot service.</i>
</div>

---

## ✨ The Problem vs The Solution

| ❌ Without this Starter | ✅ With API Standard Starter |
|---|---|
| **Response Format** varies per team/service | **Unified Envelope** (`data`, `success`, `timestamp`) |
| **Error Responses** are custom and inconsistent | **RFC 9457 `ProblemDetail`** enforced globally |
| **Debugging** requires grep-ing logs blindly | **`traceId` MDC propagation** instantly tracks requests |
| **OpenAPI Docs** need manual `@ApiResponse` on everything | **Zero Annotations** auto-generated schemas |
| **Microservice Errors** (Feign) swallowed | **Mapped** transparently to standard RFC 9457 |

---

## ⚡ Quick Start (30 Seconds)

**1. Add the dependency:**
```xml
<dependency>
    <groupId>io.github.saul789</groupId>
    <artifactId>api-standard-spring-boot-starter</artifactId>
    <version>1.2.0</version>
</dependency>
```

**2. Run your app. You're done.**
Your API is now wrapped in a standard response envelope, RFC 9457 compliant, trace-enabled, and auto-documented in Swagger!

---

## 🛠️ Core Features

### 🛡️ 1. Global Exception Handling (RFC 9457)
Intercepts all exceptions (Validation, Business, Spring Security, Feign, Resilience4j) and translates them into the rigorous `ProblemDetail` specification.

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

### 📦 2. Fluent Business Exceptions
Throw domain-specific errors gracefully using our fluent builder API:
```java
throw BusinessException.builder("error.user.not_found")
    .status(HttpStatus.NOT_FOUND)
    .code(ErrorCode.NOT_FOUND)
    .build();
```

### 🌍 3. Built-in i18n & Actuator Endpoint
- **i18n:** Resolves `detail` and `title` using Spring's `MessageSource` via the `Accept-Language` header automatically.
- **Actuator:** Check all registered error codes via `GET /actuator/api-errors`.

### 🔄 4. Zero-Config OpenAPI / Swagger
If `springdoc-openapi` is present, it auto-configures your Swagger UI. It wraps `200 OK` schemas in the ApiResponse envelope and registers `ProblemDetail` schemas for 400/500 errors—without a single `@ApiResponse` annotation.

### 🔗 5. Dynamic Documentation URIs
Easily override the default `urn:problem-type:` with real URLs pointing to your company's Developer Portal via `application.yml` or the `@ProblemType` annotation.

---

## 🔮 Coming in V2.0 (Enterprise Extensibility)
We are currently building the next generation of this starter, focusing on enterprise scale:
- 🔌 **Plugin SPI:** Inject custom metadata (like `UserId` or `TenantId`) into your errors via `spring.factories`.
- 📊 **Micrometer Metrics:** Auto-generated Grafana-ready metrics for every `ErrorCode`.
- 🔐 **Spring Security Native Integration:** Map `AccessDeniedException` effortlessly.
- 📜 **Strict Checkstyle & Javadoc Enforcement.**

---

## 🤝 Contributing & Testing
- **Postman Collection:** Available in `sample-project/postman/spring-boot-starter-api-standard.postman_collection.json`. *(Must be kept updated with every PR!)*
- **Documentation:** Interactive Docs generated via Redocly on GitHub Pages.

## 📄 License
This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

<div align="center">
  <i>Built with ❤️ for a better Spring Boot ecosystem.</i>
</div>