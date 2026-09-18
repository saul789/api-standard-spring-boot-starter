# Troubleshooting

This guide helps you diagnose and fix common issues you might encounter when integrating the `api-standard-spring-boot-starter`.

---

## Diagnostic Table

| Symptom | Probable Root Cause | Solution |
| :--- | :--- | :--- |
| **`traceId` is missing in the JSON** | The SLF4J MDC is empty when the error occurs. | Ensure you have a tracing library installed (like Micrometer Tracing) or manually populate `MDC.put("traceId", ...)` in a prior filter. Also, verify the exception didn't happen in a background thread that lost the MDC context. |
| **Response is HTML instead of JSON** | The client is requesting HTML, or you used `@Controller` instead of `@RestController`. | Ensure the client sends `Accept: application/json`. Verify your controllers are annotated with `@RestController`. |
| **Custom `ProblemDetailEnricher` doesn't execute** | Spring Boot did not detect your bean. | Verify your enricher class is annotated with `@Component` and is located within a package covered by your `@SpringBootApplication` component scan. |
| **`BusinessException` returns HTTP 500 instead of 4xx** | The exception was caught and wrapped by a transaction boundary or proxy. | Spring's `@Transactional` proxies might wrap checked exceptions in an `UndeclaredThrowableException`. Since `BusinessException` is a `RuntimeException`, ensure no intermediate aspect is swallowing and re-throwing it generically. |
| **Degraded mode (`X-API-Degraded`) never triggers** | The underlying OS or JVM does not expose CPU metrics. | If `OperatingSystemMXBean.getCpuLoad()` returns `-1`, the library defaults to "normal load". You must implement a custom `SystemLoadMonitor` if you need to use a different metric (like memory or active threads). |
| **Validation errors don't show specific fields** | The exception thrown was not `MethodArgumentNotValidException`. | Ensure you are using `@Valid` or `@Validated` on your controller payload parameters, rather than manually throwing generic exceptions. |

---

## Debugging Deep-Dive

If the table above doesn't resolve your issue, you can enable deep trace logging to see exactly how the library is evaluating the exceptions.

### Enabling Trace Logs

Add the following to your `application.yml` (development only):

```yaml
logging:
  level:
    io.github.saul789.api.standard: TRACE
```

This will log:
*   Whenever the `GlobalExceptionHandler` intercepts an error.
*   The exact `ErrorCode` resolved.
*   Failures when trying to resolve custom `URI` types or request paths.
