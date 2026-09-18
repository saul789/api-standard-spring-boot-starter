# Guías Avanzadas

Esta sección cubre escenarios empresariales avanzados. Siguiendo lineamientos estrictos de documentación, solo documentamos integraciones y patrones que están soportados explícitamente por el código base actual.

---

## Capacidades Enterprise Soportadas

### 1. Observabilidad, Tracing y Métricas (MDC, OTel y Micrometer)
La librería se integra nativamente con la infraestructura de telemetría empresarial de manera no invasiva:
*   **Correlación vía MDC**: El `TraceContextFilter` detecta y adopta los IDs de rastreo generados por agentes APM (como Datadog, New Relic, Dynatrace) o Micrometer Tracing en el MDC de SLF4J, inyectando el `traceId` en cada payload de error.
*   **Enriquecimiento Nativo de Spans de OpenTelemetry**: Si la API de OTel está en el classpath, el starter enriquece el span activo:
    *   Registra formalmente la excepción como un evento del span llamando a `recordException(ex)`.
    *   Establece el estado del span en `StatusCode.ERROR`.
    *   Inyecta atributos semánticos oficiales: `exception.type`, `exception.message`, `error.type` y `error.code`.
*   **Métricas de Falla con Micrometer**: Recolecta métricas de errores técnicos a través de `api.errors.total` etiquetadas por código, estado, método y patrón de ruta HTTP (evitando la explosión de cardinalidad de Prometheus).

### 2. Integración con Microservicios (Feign)
El starter incluye un `FeignExceptionHandler` que se activa automáticamente si `feign.FeignException` está en el classpath.
*   **Comportamiento**: Cuando un servicio upstream falla (ej. retorna 400 o 500), el handler intercepta la `FeignException`.
*   **Bubbling (Burbujeo)**: Analiza el cuerpo del error remoto (esperando un JSON RFC 9457). Si el servicio remoto retornó un `detail` y un `code`, este starter los extrae y los pasa al cliente final (bubbling), previniendo la pérdida de contexto.


### 3. Resiliencia (Circuit Breaker y Rate Limiting)
El `GlobalExceptionHandler` detecta y maneja nativamente las excepciones de Resilience4j:
*   `CallNotPermittedException` (Circuit Breaker Abierto) -> Retorna `503 Service Unavailable` con el código `SERVICE_UNAVAILABLE`.
*   `RequestNotPermitted` / `BulkheadFullException` (Rate Limiting) -> Retorna `429 Too Many Requests` con el código `TOO_MANY_REQUESTS`.

---

## Capacidades No Soportadas

Los siguientes temas **no están soportados actualmente** de forma nativa por el starter y requieren implementación personalizada a nivel de aplicación:

*   **Serverless / Edge Runtime**: La librería depende de Servlet API (`HttpServletRequest`) y `@ControllerAdvice` de Spring MVC, lo que la hace incompatible con entornos puros de edge runtime o Serverless que no usen servlets.
*   **Multitenancy (Multi-inquilino)**: No hay soporte nativo. Se puede lograr escribiendo un `ProblemDetailEnricher` personalizado.
*   **Feature Flags**: Sin integración nativa con LaunchDarkly, Unleash, etc.
