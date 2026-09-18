# API Standard Starter

Enterprise-grade API standardization and error intelligence for Spring Boot.

`api-standard-spring-boot-starter` is a library designed to standardize API error handling in Spring Boot applications by enforcing strict compliance with **RFC 9457 (Problem Details for HTTP APIs)**.

It provides a unified contract for error responses, automatic PII redaction, and a passive system load indicator.

---

## The Problem It Resolves

In distributed systems and microservices, inconsistent error responses lead to:
*   **Contract Drift**: Different endpoints or services returning different JSON structures for similar errors.
*   **Traceability Gaps**: Errors lacking a unified correlation ID, making log aggregation and debugging difficult.
*   **Information Leakage**: Accidental exposure of internal stack traces or database details in production responses.

This starter provides a centralized mechanism to handle exceptions and render them in a predictable, machine-readable format.

---

## Core Capabilities

The library provides the following features based on the current codebase:

*   **RFC 9457 Compliance**: All handled errors return a `ProblemDetail` body with standard fields (`type`, `title`, `status`, `detail`, `instance`).
*   **Error Codes**: Maps exceptions to a set of canonical error codes (e.g., `INTERNAL_ERROR`, `VALIDATION_ERROR`, `NOT_FOUND`).
*   **Trace ID Injection**: Automatically injects a `traceId` from the MDC into the error response.
*   **PII Redaction**: Masks sensitive fields (like `password`, `creditCard`, `token`) in validation error details.
*   **Degraded Mode Indicator**: Adds a header or JSON flag when system CPU usage exceeds a configurable threshold.

---

## Architecture Overview

The library operates as a non-intrusive layer on top of Spring MVC:

1.  **Interception**: A `@ControllerAdvice` (`GlobalExceptionHandler`) intercepts thrown exceptions.
2.  **Enrichment**: A pipeline of `ProblemDetailEnricher` beans (such as `StandardMetadataEnricher` and `TraceIdEnricher`) adds operational metadata to the response.
3.  **Serialization**: The final `ProblemDetail` is serialized as JSON.

---

## Benchmarks & Production Cases

> [!NOTE]
> Specific benchmark metrics (latency overhead, throughput capacity) and specific company case studies are not available in the current project source code or documentation. The library is designed to be lightweight by minimizing object allocations in the exception handling path, but no official performance numbers are declared.

---

## Quick Example

When a validation fails, the library produces a response like this:

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/v1/resource",
  "code": "VALIDATION_ERROR",
  "traceId": "d8b3a0e1c2d3e4f5",
  "timestamp": "2026-05-18T21:27:00Z"
}
```

## Next Steps

*   **[Installation](installation.md)**: Add the dependency to your project.
*   **[Quick Start](quickstart.md)**: See how to throw and handle a standard error.
