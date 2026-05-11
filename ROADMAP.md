# 🗺️ ROADMAP — spring-boot-starter-api-standard

> Actualiza este archivo cuando completes una tarea o planifiques una nueva.
> El agente de IA lo usa para ser proactivo y anticipar lo que sigue.

---

## ✅ V1.0 — Fundamentos (Completado)

- [x] Manejo global de excepciones con RFC 9457 (`GlobalExceptionHandler`)
- [x] Wrapping automático de respuestas exitosas (`ApiResponseAdvice`)
- [x] Propagación de `traceId` via MDC (`TraceContextFilter`)
- [x] i18n para mensajes de error (inglés y español)
- [x] `BusinessException` para errores de dominio
- [x] `ErrorCode` enum con mapeo automático desde HTTP status
- [x] Logging de requests (`RequestLoggingFilter`)
- [x] Publicación en Maven Central

---

## ✅ V1.1 — Enriquecimiento de Errores (Completado)

- [x] Soporte para `@ProblemType("https://...")` en excepciones custom
- [x] Soporte para `ProblemTypeProvider` (interfaz)
- [x] `type` configurable via `api.standard.errors.type-overrides.<CODE>=url`
- [x] Field `code` añadido al `ProblemDetail` (machine-readable)
- [x] `FeignExceptionHandler` para errores de clientes Feign
- [x] Auto-configuración Spring Boot (`@AutoConfiguration`)

---

## ✅ V1.2 — Integración con Ecosistema (Completado)

- [x] Soporte para Resilience4j (Circuit Breaker → 503, Rate Limiter → 429, Bulkhead → 429, Timeout → 504)
- [x] Integración condicional con springdoc-openapi / Swagger UI
- [x] Migración a Java 21 (LTS)
- [x] Migración a Spring Boot 4.x
- [x] Cobertura de tests al 95%+ con JaCoCo enforced
- [x] SpotBugs con política "Low threshold / Max effort"
- [x] GitHub Actions CI/CD pipeline

---

## 🚧 V1.3 — Hardening y DX (En progreso)

- [x] **Mejorar DX de `BusinessException`** → Agregar builder fluido para simplificar su construcción
- [x] **Soporte para `@Validated` en nivel de clase** (no solo parámetros de método) - Ya manejado vía `ConstraintViolationException`
- [x] **Actuator endpoint** para listar todos los `ErrorCode` registrados y sus configuraciones
- [x] **Documentación interactiva en GitHub Pages** generada desde el `sample-project` (Workflow `docs.yml` usando Redocly)
- [x] **Google Java Format** — Formato aplicado (`mvn fmt:format` ejecutado y plugins restaurados para Java 21)
- [x] **PMD** — Resolver las violations iniciales del ruleset `quickstart.xml` en el módulo `starter/` (19 rules corregidas)

---

## 🔮 V2.0 — Extensibilidad y Enterprise (Planificado)

- [ ] **Plugin SPI (Service Provider Interface):** Permitir a los usuarios registrar `ProblemDetailEnricher` propios via `spring.factories` o AutoConfiguración para inyectar metadatos customizados (ej. User ID) en los errores.
- [ ] **Módulo de métricas:** Integración automática con Micrometer para contar excepciones por `ErrorCode`, HTTP method y path (preparado para Grafana/Prometheus).
- [ ] **Integración Nativa con Spring Security:** Mapeo automático de `AccessDeniedException` y `AuthenticationException` al estándar RFC 9457 desde los filtros de seguridad.
- [ ] **Checkstyle Integration:** Imposición de convenciones de nombrado, orden de imports y Javadocs **obligatorios en inglés** para todo el código público del starter (todo Javadoc existente en español se migrará a inglés).
- [ ] **Soporte para WebFlux** (reactive stack, no solo Servlet)
- [ ] **Rate Limiting nativo** sin depender de Resilience4j
- [ ] **Modo estricto:** Fail-fast en startup si hay `ErrorCode` sin entrada i18n

---

## 💡 Ideas / Backlog (Sin comprometer)

- [ ] Integración con OpenTelemetry para `traceId` automático (en lugar de MDC manual)
- [ ] Soporte para `application/problem+json` como Content-Type en la respuesta
- [ ] Exportar esquema JSON de todos los errores posibles (para documentación automática)
- [ ] Anotación `@StandardResponse` para marcar controladores que deben envolverse

---

## 📜 Seguimiento de Estándares RFC

> Monitorear estos RFCs para anticipar cambios que puedan afectar la librería.

| RFC | Título | Estado | Última revisión | Impacto en el proyecto |
|---|---|---|---|---|
| **[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)** | Problem Details for HTTP APIs | 🟢 Vigente | 2023 | ⭐ **CRÍTICO** — Es el formato base de todos los errores |
| **[RFC 9110](https://www.rfc-editor.org/rfc/rfc9110)** | HTTP Semantics | 🟢 Vigente | 2022 | 🔴 **ALTO** — Define la semántica de los status codes en `ErrorCode.java` |
| **[RFC 6585](https://www.rfc-editor.org/rfc/rfc6585)** | Additional HTTP Status Codes | 🟢 Vigente | 2012 | 🟡 **MEDIO** — Origen del `429 Too Many Requests` |
| **[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)** | Problem Details (obsoleto) | 🔴 Obsoleto | Reemplazado por RFC 9457 | ⚠️ **VIGILAR** — No implementar. Existente en APIs legadas |
| **[RFC 4918](https://www.rfc-editor.org/rfc/rfc4918)** | HTTP Extensions for WebDAV | 🟡 Estático | 2007 | 🟡 **BAJO** — Origen del `422 Unprocessable Entity` |

### Checklist de cumplimiento RFC (verificar en cada release)

- [ ] **RFC 9457:** Los campos `type`, `title`, `status` siempre presentes en toda respuesta de error
- [ ] **RFC 9457:** El campo `type` es una URI válida (URL o URN bien formada)
- [ ] **RFC 9110:** Los códigos HTTP usados corresponden a la semántica definida (no usar `500` cuando corresponde `503`)
- [ ] **RFC 6585:** `429` solo se usa para rate limiting / throttling, no para errores genéricos de cliente
- [ ] **RFC 4918:** `422` solo para validación semántica; `400` para syntax errors / requests malformados
- [ ] **RFC 7807:** No hay referencias a RFC 7807 en la documentación pública (usar siempre RFC 9457)

---

## 📊 Estado actual

| Métrica | Valor |
|---|---|
| Cobertura de tests (JaCoCo) | ≥ 95% (enforced) |
| Java | 21 LTS |
| Spring Boot | 4.0.3 |
| Versión publicada | 1.2.0 |
| Tests en GlobalExceptionHandlerTest | 621 líneas / ~30 casos |
