# Advanced Guides

This section covers advanced enterprise scenarios. Following strict documentation guidelines, we only document integrations and patterns explicitly supported by the current codebase.

---

## Supported Enterprise Capabilities

### 1. Observability, Tracing, and Metrics (MDC, OTel, and Micrometer)
The library integrates natively with enterprise telemetry infrastructure in a non-invasive manner:
*   **MDC Correlation**: `TraceContextFilter` automatically detects and adopts trace IDs injected by APM agents (like Datadog, New Relic, Dynatrace) or Micrometer Tracing into the SLF4J MDC, embedding the `traceId` inside every error payload.
*   **Native OpenTelemetry Span Enrichment**: If the OTel API is on the classpath, the starter enriches the active span:
    *   Registers the exception as a native span event by calling `recordException(ex)`.
    *   Marks the active span status as `StatusCode.ERROR`.
    *   Injects official semantic attributes: `exception.type`, `exception.message`, `error.type`, and `error.code`.
*   **Failure Metrics with Micrometer**: Collects technical failure metrics under the `api.errors.total` counter tagged by error code, status, request method, and HTTP path pattern (safeguarded against Prometheus cardinality explosions).

### 2. Microservices Integration (Feign)
The starter includes a `FeignExceptionHandler` that activates automatically if `feign.FeignException` is on the classpath.
*   **Behavior**: When an upstream service fails (e.g., returns a 400 or 500), the handler intercepts the `FeignException`.
*   **Bubbling**: It parses the remote error body (expecting an RFC 9457 JSON). If the remote service returned a `detail` and `code`, this starter extracts them and bubbles them up to the caller, preventing context loss.


### 3. Resiliency (Circuit Breaker & Rate Limiting)
The `GlobalExceptionHandler` natively detects and handles Resilience4j exceptions:
*   `CallNotPermittedException` (Circuit Breaker Open) -> Returns `503 Service Unavailable` with `SERVICE_UNAVAILABLE` code.
*   `RequestNotPermitted` / `BulkheadFullException` (Rate Limiting) -> Returns `429 Too Many Requests` with `TOO_MANY_REQUESTS` code.

---

## Unsupported Capabilities

The following topics are **not currently supported** natively by the starter and require custom implementation at the application level:

*   **Serverless / Edge Runtime**: The library relies on Servlet API (`HttpServletRequest`) and Spring MVC `@ControllerAdvice`, making it incompatible with pure edge runtimes or non-servlet Serverless environments.
*   **Multitenancy**: No native tenant-awareness. Can be achieved by writing a custom `ProblemDetailEnricher`.
*   **Feature Flags**: No native integration with LaunchDarkly, Unleash, etc.
