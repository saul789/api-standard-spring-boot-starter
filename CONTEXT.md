# 📋 CONTEXT.md — spring-boot-starter-api-standard

> **Propósito:** Este archivo le da contexto instantáneo al agente de IA para trabajar con máxima autonomía.
> Actualízalo cuando cambies versiones, tomes decisiones de arquitectura, o completes hitos del roadmap.

---

## 🧩 Identidad del Proyecto

| Campo | Valor |
|---|---|
| **GroupId** | `io.github.saul789` |
| **ArtifactId** | `spring-boot-starter-api-standard` |
| **Versión actual** | `1.2.0` |
| **Java** | 21 (LTS) |
| **Spring Boot** | 4.0.3 |
| **Licencia** | Apache 2.0 |
| **GitHub** | https://github.com/saul789/spring-boot-starter-api-standard |

---

## 🏛️ Arquitectura Clave

### Módulos del proyecto
```
spring-boot-starter-api-standard/   ← root pom (parent)
├── starter/                         ← librería principal (publicada en Maven Central)
└── sample-project/                  ← demo de uso de la librería
```

### Paquete base
```
io.github.saul789.api.standard
├── exception/          ← GlobalExceptionHandler, BusinessException, ErrorCode, ProblemDetailService...
├── advice/             ← ApiResponseAdvice (envuelve respuestas exitosas)
├── filter/             ← RequestLoggingFilter, TraceContextFilter
├── model/              ← ValidationError, respuestas estándar
├── openapi/            ← Integración condicional con springdoc-openapi
└── ApiStandardProperties.java / ApiStandardAutoConfiguration.java
```

### Flujo de una excepción
```
Controller → throws Exception
    → GlobalExceptionHandler.handle*()
    → ProblemDetailService.createProblem()
    → [ProblemDetailEnricher] → StandardMetadataEnricher, TraceIdEnricher
    → ProblemDetail (RFC 9457) ← respuesta JSON al cliente
```

---

## 📐 Decisiones de Arquitectura (NO cambiar sin discutir)

1. **Formato de error:** Siempre RFC 9457 (`ProblemDetail`). NUNCA devolver errores en formato propio.
2. **Campo `type`:**
   - Por defecto: URN autogenerado → `urn:problem-type:<kebab-case-code>` (ej: `urn:problem-type:not-found`)
   - Personalizable via `ProblemException(url, ...)` o `@ProblemType("https://...")`
   - Configurable globalmente via `api.standard.errors.type-overrides.<CODE>=https://...`
3. **`traceId`:** Se propaga via MDC (`org.slf4j.MDC`). Siempre debe aparecer en la respuesta si está disponible.
4. **i18n:** Los mensajes de error se resuelven via `MessageSource`. Las claves siguen el patrón `error.<KEY>`.
5. **Extensión:** Para agregar nuevos enrichers implementar `ProblemDetailEnricher` (NO modificar `ProblemDetailService` directamente).
6. **Resilience4j:** Manejado por nombre de clase (sin dependencia directa) para ser opcional. Ver `GlobalExceptionHandler.handleGenericException()`.
7. **Cobertura mínima:** **95% instruction coverage** (configurado en JaCoCo del `pom.xml` raíz). ⚠️ No reducir.
8. **`@Order(Ordered.LOWEST_PRECEDENCE)`:** El `GlobalExceptionHandler` tiene la menor prioridad para que handlers más específicos (como `FeignExceptionHandler`) tengan precedencia.

---

## 📚 Estándares RFC de Referencia

> Estos RFCs son la base normativa del proyecto. Consultar antes de modificar el comportamiento de errores HTTP.

| RFC | Título | Estado | Aplicación en este proyecto |
|---|---|---|---|
| **[RFC 9457](https://www.rfc-editor.org/rfc/rfc9457)** | Problem Details for HTTP APIs | 🟢 Vigente (2023) | ⭐ **Core de la librería.** Define el formato `ProblemDetail` con campos `type`, `title`, `status`, `detail`, `instance` |
| **[RFC 9110](https://www.rfc-editor.org/rfc/rfc9110)** | HTTP Semantics | 🟢 Vigente (2022) | Define la semántica oficial de **todos los HTTP status codes** usados en `ErrorCode.java`. Reemplaza RFC 7231 |
| **[RFC 6585](https://www.rfc-editor.org/rfc/rfc6585)** | Additional HTTP Status Codes | 🟢 Vigente (2012) | Define **`429 Too Many Requests`** → mapeado a `ErrorCode.TOO_MANY_REQUESTS` (Rate Limiter, Bulkhead) |
| **[RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)** | Problem Details for HTTP APIs | 🔴 Obsoleto (reemplazado por RFC 9457) | Versión anterior de RFC 9457. **No implementar.** Documentado para evitar regresiones |
| **[RFC 4918](https://www.rfc-editor.org/rfc/rfc4918)** | HTTP Extensions for WebDAV | 🟡 Nicho (2007) | Define **`422 Unprocessable Entity`** → mapeado a `ErrorCode.VALIDATION_ERROR` en validaciones semánticas |

### Notas de cumplimiento

- ✅ **RFC 9457 compliant:** Cada `ProblemDetail` generado incluye los campos obligatorios (`type`, `title`, `status`) y los extensibles (`code`, `traceId`, `timestamp`)
- ✅ **RFC 9110 compliant:** Los HTTP status codes usados en `ErrorCode.fromStatus()` siguen la semántica oficial
- ✅ **RFC 6585 compliant:** `429` se usa exclusivamente para rate limiting y bulkhead (no para errores genéricos)
- ✅ **RFC 4918 compliant:** `422` se reserva para validaciones semánticas; `400` para requests malformados
- ⛔ **RFC 7807 deprecated:** Si alguien referencia RFC 7807, redirigir a RFC 9457

---

## 🔑 Archivos Críticos (Leer antes de modificar)

| Archivo | Propósito |
|---|---|
| `starter/src/main/java/.../exception/GlobalExceptionHandler.java` | Corazón del manejo de errores. Mapea excepciones → ProblemDetail |
| `starter/src/main/java/.../exception/ProblemDetailService.java` | Construye y enriquece el ProblemDetail |
| `starter/src/main/java/.../exception/ErrorCode.java` | Enum con todos los códigos de error + mapeo por HTTP status |
| `starter/src/main/java/.../exception/BusinessException.java` | Excepción que los devs lanzan desde su código de negocio |
| `starter/src/main/java/.../exception/FeignExceptionHandler.java` | Handler específico para errores de clientes Feign |
| `starter/src/main/java/.../exception/ResilienceExceptionHandler.java` | Handler para Resilience4j (Circuit Breaker, Rate Limiter, etc.) |
| `starter/src/main/resources/i18n/messages.properties` | Mensajes de error en inglés |
| `starter/src/main/resources/i18n/messages_es.properties` | Mensajes de error en español |
| `starter/src/test/java/.../exception/GlobalExceptionHandlerTest.java` | Test principal (621 líneas). Sirve como referencia de patrones |
| `ApiStandardAutoConfiguration.java` | Auto-configuración Spring Boot (beans registrados) |
| `ApiStandardProperties.java` | Propiedades configurables via `application.properties` |

---

## ✅ Errorcodes disponibles y sus HTTP Status

| ErrorCode | HTTP Status | i18n key (título) |
|---|---|---|
| `INTERNAL_ERROR` | 500 | `error.INTERNAL_ERROR` |
| `BAD_REQUEST` | 400 | `error.BAD_REQUEST` |
| `VALIDATION_ERROR` | 400/422 | `error.VALIDATION_ERROR` |
| `UNAUTHORIZED` | 401 | `error.UNAUTHORIZED` |
| `FORBIDDEN` | 403 | `error.FORBIDDEN` |
| `NOT_FOUND` | 404 | `error.NOT_FOUND` |
| `METHOD_NOT_ALLOWED` | 405 | `error.METHOD_NOT_ALLOWED` |
| `CONFLICT` | 409 | `error.CONFLICT` |
| `GONE` | 410 | — |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | `error.UNSUPPORTED_MEDIA_TYPE` |
| `TOO_MANY_REQUESTS` | 429 | — |
| `BAD_GATEWAY` | 502 | — |
| `SERVICE_UNAVAILABLE` | 503 | — |
| `GATEWAY_TIMEOUT` | 504 | — |

---

## 🚫 Lo que NUNCA debes hacer

- ❌ Usar `INTERNAL_ERROR` si existe un código más específico para la situación
- ❌ Devolver errores sin el campo `type` en el JSON
- ❌ Romper el contrato RFC 9457 al refactorizar
- ❌ Bajar la cobertura de tests por debajo del 95%
- ❌ Agregar un nuevo `ErrorCode` sin su entrada en `messages.properties` y `messages_es.properties`
- ❌ Modificar `ProblemDetailService` directamente para agregar lógica de enriquecimiento (usar `ProblemDetailEnricher`)
- ❌ Dejar los archivos `.log` en la raíz del proyecto
- ❌ **Cambiar la versión del `sample-project` cuando el cambio es solo en `starter/`** (y viceversa) — son módulos independientes con ciclos de vida distintos

---

## 📦 Formato de respuesta de error (RFC 9457)

```json
{
  "type": "urn:problem-type:not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "The user with ID 42 was not found.",
  "instance": "/api/users/42",
  "code": "NOT_FOUND",
  "traceId": "abc-123-xyz",
  "timestamp": "2026-05-10T17:00:00Z"
}
```

---

## 🧪 Patrón de Testing (referencia del proyecto)

### Setup estándar para nuevos tests
```java
@BeforeEach
void setUp() {
    this.messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
            .thenAnswer(invocation -> invocation.getArgument(2)); // devuelve el fallback

    this.properties = new ApiStandardProperties();
    List<ProblemDetailEnricher> enrichers = List.of(
            new StandardMetadataEnricher(),
            new TraceIdEnricher());
    this.problemDetailService = new ProblemDetailService(messageSource, properties, enrichers);
    this.handler = new GlobalExceptionHandler(messageSource, problemDetailService);
    this.request = mock(HttpServletRequest.class);
}
```

### Qué siempre verificar en un test de error
```java
// Campos RFC 9457 obligatorios
assertEquals(HttpStatus.XXX.value(), result.getStatus());    // HTTP status correcto
assertNotNull(result.getType());                              // type siempre presente
assertNotNull(result.getProperties().get("code"));           // code machine-readable
assertNotNull(result.getProperties().get("timestamp"));      // timestamp siempre
// traceId: solo si MDC.put("traceId", value) fue llamado antes
```

---

## ⚙️ Comandos útiles

```bash
# Build completo con tests y cobertura (el comando definitivo antes de un PR)
mvn clean verify

# Solo tests (rápido)
mvn test

# Verificar cobertura JaCoCo
mvn verify   # el reporte queda en starter/target/site/jacoco/index.html

# Verificar SpotBugs (análisis estático)
mvn spotbugs:check

# Instalar en repositorio local (para probar en sample-project)
mvn clean install -DskipTests
```

---

## 🌿 Convención de branches

| Branch | Propósito |
|---|---|
| `main` | Producción (releases publicados en Maven Central) |
| `develop` | Integración de features en curso |
| `feature/<nombre>` | Features nuevas |
| `fix/<nombre>` | Bug fixes |

---

## 📝 Conventional Commits (Estándar de mensajes de commit)

**Spec oficial:** https://www.conventionalcommits.org/en/v1.0.0/

### Formato
```
<type>(<scope>): <descripción corta en imperativo>

[body opcional — explica el "por qué", no el "qué"]

[footer opcional — BREAKING CHANGE, referencias a issues]
```

### Tipos válidos (`type`)

| Type | Cuándo usarlo | Impacto en SemVer |
|---|---|---|
| `feat` | Nueva funcionalidad | **minor** (`1.2.0` → `1.3.0`) |
| `fix` | Corrección de bug | **patch** (`1.2.0` → `1.2.1`) |
| `docs` | Solo documentación | ninguno |
| `test` | Agregar/corregir tests | ninguno |
| `refactor` | Refactor sin cambio de comportamiento | ninguno |
| `perf` | Mejora de rendimiento | **patch** |
| `chore` | Mantenimiento (deps, config) | ninguno |
| `ci` | Cambios en GitHub Actions | ninguno |
| `build` | Sistema de build (pom.xml, jacoco) | ninguno |
| `style` | Formato de código sin cambio de lógica | ninguno |
| `revert` | Revertir un commit anterior | depende |

> ⚠️ **BREAKING CHANGE** → major (`1.2.0` → `2.0.0`). Se indica con `!` después del type o con footer `BREAKING CHANGE:`.

### Scopes del proyecto (`scope`)

| Scope | Aplica a |
|---|---|
| `exception` | `GlobalExceptionHandler`, `BusinessException`, `ErrorCode`, `ProblemException` |
| `handler` | `FeignExceptionHandler`, `ResilienceExceptionHandler` |
| `service` | `ProblemDetailService`, `ProblemDetailEnricher` y enrichers |
| `advice` | `ApiResponseAdvice` |
| `filter` | `RequestLoggingFilter`, `TraceContextFilter` |
| `config` | `ApiStandardAutoConfiguration`, `ApiStandardProperties` |
| `i18n` | `messages.properties`, `messages_es.properties` |
| `test` | Archivos de test |
| `docs` | `README.md`, `CONTEXT.md`, `ROADMAP.md` |
| `ci` | Workflows de GitHub Actions |
| `deps` | Dependencias en `pom.xml` |
| `sample` | `sample-project/` |

### Ejemplos reales para este proyecto

```bash
# Nueva funcionalidad
feat(exception): add REQUEST_TIMEOUT error code with 408 mapping

# Bug fix
fix(handler): BusinessException custom URL ignored when type-override configured

# Tests
test(exception): add edge cases for ResilienceExceptionHandler bulkhead

# Documentación
docs(context): update RFC standards tracking section

# Dependencias
chore(deps): upgrade jacoco-maven-plugin to 0.8.14

# Refactor
refactor(service): extract resolveType logic to dedicated method

# CI/CD
ci(github): add spotbugs check to PR validation workflow

# Build
build(pom): raise minimum coverage threshold to 95 percent

# BREAKING CHANGE (renombrar algo de la API pública)
feat(exception)!: rename ProblemException constructor param url to typeUri

# Con body y footer
feat(exception): add support for PaymentException

Adds PAYMENT_FAILED error code mapped to HTTP 402.
Includes i18n entries for EN and ES.

Closes #42
```

### Relación con SemVer y releases

Este proyecto sigue [Semantic Versioning](https://semver.org/):

| Commits desde último release | Versión resultante |
|---|---|
| Solo `fix`, `perf` | patch → `1.2.0` → `1.2.1` |
| Al menos un `feat` | minor → `1.2.0` → `1.3.0` |
| Cualquier `BREAKING CHANGE` | major → `1.2.0` → `2.0.0` |
| Solo `docs`, `chore`, `ci`, `test` | sin release |

---

## 🔢 Semantic Versioning (SemVer 2.0.0)

**Spec oficial:** https://semver.org/

### Formato
```
MAJOR.MINOR.PATCH[-PRERELEASE]
```

| Segmento | Cuándo incrementar | Ejemplo |
|---|---|---|
| **MAJOR** | Cambio que rompe compatibilidad (breaking change) | `1.2.0` → `2.0.0` |
| **MINOR** | Nueva funcionalidad compatible hacia atrás | `1.2.0` → `1.3.0` |
| **PATCH** | Bug fix compatible hacia atrás | `1.2.0` → `1.2.1` |

### Pre-releases
```
1.3.0-alpha.1   ← inestable, en desarrollo
1.3.0-beta.1    ← funcionalidad completa, puede tener bugs
1.3.0-rc.1      ← Release Candidate, lista para QA final
1.3.0           ← release estable ✅
```

### Qué es un BREAKING CHANGE en este proyecto

Sube el MAJOR si se modifica cualquiera de estas **APIs públicas**:

| Elemento | Por qué es breaking |
|---|---|
| Firma de `BusinessException` / `ProblemException` | Los usuarios los instancian directamente |
| Interfaz `ProblemDetailEnricher` | Los usuarios la implementan para extender la librería |
| Interfaz `ProblemTypeProvider` | Los usuarios la implementan en sus excepciones |
| Anotación `@ProblemType` | Los usuarios la aplican en sus clases |
| Propiedades de `ApiStandardProperties` | Renombrar/eliminar propiedades rompe `application.properties` de los usuarios |
| Campos del JSON de error (`code`, `traceId`, `timestamp`) | Los clientes de la API los consumen y pueden depender de ellos |
| Enum `ErrorCode` — eliminar un valor | Los usuarios referencian `ErrorCode.XXX` en su código |

### ⚠️ Regla crítica: Independencia de versiones por módulo

Este repositorio tiene **dos módulos con ciclos de vida independientes**:

| Módulo | ArtifactId | Publicado en | Versión |
|---|---|---|---|
| `starter/` | `api-standard-spring-boot-starter` | Maven Central ✅ | Sigue SemVer estricto |
| `sample-project/` | `sample-project` | Solo local / GitHub | Versión libre (demo) |

**Reglas de independencia:**
- 🔴 Si el cambio es **solo en `starter/`** → solo actualizar la versión del parent/starter. **No tocar `sample-project/`**.
- 🔴 Si el cambio es **solo en `sample-project/`** → solo actualizar esa referencia. **No tocar la versión del starter**.
- 🟢 Si el cambio afecta **a ambos** (ej: el sample-project adopta una nueva feature del starter) → actualizar ambos de forma coordinada y documentarlo.

### Comandos para cambiar versión

```bash
# Actualizar solo la versión del starter (root pom + starter/pom)
mvn versions:set -DnewVersion=1.3.0 -pl . -DupdateMatchingVersions=false

# Verificar qué versiones hay en el proyecto
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
```

---

## 🛠️ Herramientas de Calidad de Código

### Stack completo de análisis estático

| Herramienta | Tipo | Estado | Propósito |
|---|---|---|---|
| **SpotBugs** | Bug detector | ✅ Configurado (`pom.xml`) | Detecta bugs reales: null derefs, resource leaks, race conditions |
| **JaCoCo** | Coverage | ✅ Configurado (`pom.xml`) | Enforces ≥ 95% instruction coverage |
| **PMD** | Code quality | ✅ Activo (`pom.xml`) | Detecta código muerto, complejidad excesiva, bad practices |
| **Google Java Format** | Formatter | ✅ Activo (`pom.xml`) | Formato de código 100% determinístico y consistente |
| **Checkstyle** | Convenciones | ⬜ Planificado para V2.0 | Valida convenciones de nombres, Javadoc, imports |
| **OWASP Dependency Check** | Seguridad | ✅ Script `scripts/dependency-check.ps1` | Detecta vulnerabilidades en dependencias |

### Configuración actual de SpotBugs
```xml
<!-- pom.xml raíz -->
<configuration>
    <effort>Max</effort>              <!-- Análisis exhaustivo -->
    <threshold>Low</threshold>        <!-- Reporta hasta bugs de baja confianza -->
    <failOnError>true</failOnError>   <!-- El build falla si hay bugs -->
    <excludeFilterFile>spotbugs-exclude.xml</excludeFilterFile>
</configuration>
```

### EditorConfig (base de todos los formatters)
El archivo `.editorconfig` debe existir en la raíz para que todos los IDEs usen la misma configuración:
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
trim_trailing_whitespace = true
insert_final_newline = true

[*.xml]
indent_size = 4

[*.yml]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

---

## ✨ Buenas Prácticas de Código Java (Java 21)

Estas prácticas son **obligatorias** en el módulo `starter/`. El `sample-project` las sigue como referencia.

### 1. Javadoc — Obligatorio en toda API pública
```java
// ✅ Correcto — toda clase y método público tiene Javadoc
/**
 * Signals a business-rule violation that should be communicated to the caller.
 *
 * @param code    the machine-readable error code
 * @param message the user-friendly detail message (or i18n key)
 * @param status  the HTTP status to return
 */
public BusinessException(ErrorCode code, String message, HttpStatus status) { ... }

// ❌ Incorrecto — método público sin Javadoc
public ProblemDetail handleProblemException(ProblemException ex, ...) { ... }
```

### 2. Final fields — Inmutabilidad por defecto
```java
// ✅ Correcto — campos inyectados siempre final
public class GlobalExceptionHandler {
    private final MessageSource messageSource;
    private final ProblemDetailService problemDetailService;
}

// ❌ Incorrecto — campo mutable sin razón
private MessageSource messageSource; // puede ser reasignado accidentalmente
```

### 3. Records (Java 16+) — Para DTOs simples
```java
// ✅ Correcto — ValidationError es un DTO de solo lectura, ideal como record
public record ValidationError(String field, String message) {}

// Uso:
var error = new ValidationError("email", "formato inválido");
error.field();   // getter automático
error.message(); // getter automático
```

### 4. Sealed Classes (Java 17+) — Para jerarquías cerradas
```java
// Candidato: ProblemException tiene subclases conocidas y cerradas
public sealed class ProblemException extends RuntimeException
    permits BusinessException {
    // Solo BusinessException puede extender ProblemException
}
```
> **Nota:** Adoptar en V2.0 — es un BREAKING CHANGE (cambia la API pública).

### 5. Pattern Matching (Java 21) — En lugar de instanceof + cast
```java
// ✅ Ya usas esto en GlobalExceptionHandler.handleGenericException()
if (ex instanceof ErrorResponse errorResponse) {
    ProblemDetail problem = errorResponse.updateAndGetBody(messageSource, locale);
    // ... errorResponse ya está casteado automáticamente
}

// ❌ El estilo antiguo que ya no se debe usar
if (ex instanceof ErrorResponse) {
    ErrorResponse errorResponse = (ErrorResponse) ex; // cast manual redundante
}
```

### 6. No Magic Numbers — Constantes con nombre
```java
// ✅ Correcto — usar constantes semánticas
return problemDetailService.createProblem(
    HttpStatus.BAD_REQUEST,           // no "400"
    request,
    "error.validation.body",          // no string hardcodeado anónimo
    ErrorCode.VALIDATION_ERROR.name(), // no "VALIDATION_ERROR" literal
    ex, locale);

// ❌ Incorrecto
return problemDetailService.createProblem(400, request, "Invalid body", "VAL_ERR", ex, locale);
```

### 7. Fail Fast — Validar parámetros al inicio
```java
// ✅ Correcto — fallar inmediatamente si hay inputs inválidos
public ProblemDetailService(MessageSource messageSource,
                            ApiStandardProperties properties,
                            List<ProblemDetailEnricher> enrichers) {
    this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
    this.properties    = Objects.requireNonNull(properties, "properties must not be null");
    this.enrichers     = List.copyOf(enrichers); // copia defensiva e inmutable
}

// ❌ Incorrecto — el null explota más tarde y es difícil de debuggear
public ProblemDetailService(MessageSource messageSource, ...) {
    this.messageSource = messageSource; // NullPointerException tardío e inesperado
}
```

### Checklist pre-commit de código Java
Antes de hacer commit de código nuevo, verificar:
- [ ] ¿Todos los métodos y clases públicas nuevas tienen Javadoc?
- [ ] ¿Los campos de clase son `final` donde corresponde?
- [ ] ¿Se usó `Objects.requireNonNull()` en constructores con dependencias?
- [ ] ¿No hay magic numbers/strings — se usaron constantes o enums?
- [ ] ¿Se aplicó pattern matching donde hay `instanceof`?
- [ ] ¿El build pasa con `mvn clean verify`? (SpotBugs + JaCoCo + tests)
