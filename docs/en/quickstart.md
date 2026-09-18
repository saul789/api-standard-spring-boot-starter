# Quick Start

This guide will help you create a minimal, realistic project to see the `api-standard-spring-boot-starter` in action.

---

## 1. Project Structure

A typical Spring Boot project using this starter looks like this:

```text
my-api/
├── pom.xml (or build.gradle)
└── src/
    └── main/
        ├── java/
        │   └── com/example/demo/
        │       ├── DemoApplication.java
        │       └── controller/
        │           └── DemoController.java
        └── resources/
            └── application.yml
```

---

## 2. Implementation

We will use the native `BusinessException` and `ErrorCode` provided by the library to simulate a business rule violation (e.g., trying to create a resource that already exists).

Create the controller in `DemoController.java`:

```java
package com.example.demo.controller;

import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/api/test-error")
    public void triggerError() {
        // Line-by-line explanation:
        // 1. We throw a BusinessException provided by the starter.
        // 2. We pass the canonical ErrorCode.CONFLICT (maps to 409).
        // 3. We provide a clear, user-friendly message.
        // 4. We set the HTTP status to CONFLICT (409).
        throw new BusinessException(
            ErrorCode.CONFLICT, 
            "The requested resource is already in use.",
            HttpStatus.CONFLICT
        );
    }
}
```

---

## 3. The Runtime Flow

When a client makes a request to `/api/test-error`, the following sequence occurs automatically without any extra code:

1.  **Exception Thrown**: `DemoController` throws `BusinessException`.
2.  **Interception**: The library's `GlobalExceptionHandler` (registered with the lowest precedence) catches the exception.
3.  **Processing**: `ProblemDetailService` starts building the RFC 9457 response.
4.  **Enrichment Pipeline**: The library executes the active enrichers present in the codebase:
    *   `StandardMetadataEnricher`: Adds the machine-readable `code` and the `timestamp`.
    *   `TraceIdEnricher`: Extracts the trace ID from the log MDC and injects it.
5.  **Response**: A `ProblemDetail` JSON is serialized and returned to the client.

---

## 4. Expected Result

Make a request to the endpoint (e.g., using `curl`). The starter will intercept the exception and return a clean, structured RFC 9457 compliant JSON:

```json
{
  "type": "urn:problem-type:conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "The requested resource is already in use.",
  "instance": "/api/test-error",
  "code": "CONFLICT",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "timestamp": "2026-05-18T21:34:00Z"
}
```

This ensures that all errors in your application follow the exact same contract.
