# Referencia de API

Esta sección proporciona una referencia de API rigurosa y de nivel de producción para los componentes principales de `api-standard-spring-boot-starter`.

---

## 1. `BusinessException`

Señala una violación a las reglas de negocio que debe ser comunicada al cliente con un código de estado HTTP específico y un `ErrorCode`.

### Firma Completa

```java
public class BusinessException extends ProblemException {
  public BusinessException(ErrorCode code, String message, HttpStatus status);
  public BusinessException(String message, HttpStatus status);
  public BusinessException(String message, HttpStatus status, String customUrl);
  public BusinessException(ErrorCode code, String message, HttpStatus status, String type);
  public BusinessException(ErrorCode code, String message, HttpStatus status, URI type);
  
  public static Builder builder(String message);
}
```

### Parámetros (Constructor Principal)

| Nombre | Tipo | Requerido | Default | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `code` | `ErrorCode` | Sí | Derivado del status | El código de error canónico legible por máquina. |
| `message` | `String` | Sí | - | Mensaje de detalle amigable o llave de traducción (i18n). |
| `status` | `HttpStatus` | Sí | `BAD_REQUEST` | El estado HTTP a retornar al cliente. |

### Retorna y Contratos
*   **Shape**: Extiende de `RuntimeException`.
*   **Mutabilidad**: Inmutable una vez instanciada.
*   **Ciclo de vida (Lifecycle)**: Efímero. Creada en el momento de la falla, capturada por `GlobalExceptionHandler` y eliminada por el Garbage Collector inmediatamente después de que se renderiza la respuesta.
*   **Propiedad (Ownership)**: Pertenece a la capa de dominio/aplicación que la lanza.

### Throws
Como es una excepción en sí misma, su propósito es ser lanzada.
*   **Reintentos (Retryability)**: Generalmente **no es reintentable** sin modificar la petición (ya que representa una regla de negocio violada, ej. 409 Conflict o 422 Validation Error).
*   **Recuperabilidad**: Manejada automáticamente por el handler global del starter.

### Rendimiento (Performance)
*   **Complejidad**: $O(1)$.
*   **Allocations**: Mínimo, pero involucra la generación del stack trace estándar de la JVM, lo cual es relativamente pesado.
*   **Impacto Runtime**: Diseñada para flujos excepcionales. No debe usarse para flujo de control normal para evitar el costo de generar el stack trace.

### Seguridad
*   **Validación**: Requiere un mensaje no nulo si se usa el Builder.
*   **Patrones Inseguros**: **No** pases entrada cruda del usuario o datos sensibles (PII) directamente en el campo `message` a menos que sea una llave de traducción, ya que este mensaje se expone directamente al cliente HTTP.

### Edge Cases
*   **Nulabilidad (Nullability)**: Si se pasa `code` como nulo en el constructor, se resuelve a un valor por defecto basado en el estado HTTP.
*   **Fallback de Traducción**: Si el `message` está destinado a ser una llave i18n pero la llave no se encuentra en `messages.properties`, la llave misma se retorna al usuario.

### Ejemplos

**Ejemplo Básico**
```java
if (usuario.estaSuspendido()) {
    throw new BusinessException(ErrorCode.FORBIDDEN, "El usuario está suspendido", HttpStatus.FORBIDDEN);
}
```

**Ejemplo Avanzado (Builder)**
```java
throw BusinessException.builder("error.pago.fondos_insuficientes")
    .status(HttpStatus.PAYMENT_REQUIRED)
    .code(ErrorCode.VALIDATION_ERROR)
    .type(URI.create("https://docs.miempresa.com/errores/fondos"))
    .build();
```

---

## 2. `ProblemDetailEnricher`

El hook de extensión principal para agregar metadatos personalizados a todas las respuestas de error RFC 9457.

### Firma Completa

```java
public interface ProblemDetailEnricher {
  default void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale);
  default void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale, Exception ex);
  default int order();
}
```

### Parámetros

| Nombre | Tipo | Requerido | Default | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `problem` | `ProblemDetail` | Sí | - | El objeto problem detail mutable a enriquecer. |
| `request` | `HttpServletRequest` | Sí | - | El contexto de la petición HTTP actual. |
| `locale` | `Locale` | Sí | - | El locale resuelto para la petición. |
| `ex` | `Exception` | No | null | La excepción original que disparó el error. |

### Retorna y Contratos
*   **Mutabilidad**: Las implementaciones **deben** mutar el objeto `ProblemDetail` inyectado (típicamente usando `problem.setProperty()`).
*   **Lifecycle**: Los beans que implementan esta interfaz son recolectados al inicio de la aplicación. El método `enrich` se llama de forma síncrona en cada petición de error.
*   **Orden**: Gobernado por el método `order()` (valores más bajos se ejecutan primero).

### Rendimiento (Performance)
*   **Cuellos de botella (Bottlenecks)**: Debido a que esto se ejecuta síncronamente en el hilo del Servlet durante el manejo del error, las implementaciones **no deben** realizar operaciones de entrada/salida (I/O) bloqueantes (ej. llamadas a base de datos o peticiones HTTP).
*   **Caching**: Si el enriquecimiento requiere datos de referencia, estos deben ser cacheados agresivamente en memoria.

### Seguridad
*   **Sanitización**: Cualquier dato extraído de `HttpServletRequest` (ej. headers) e inyectado en la respuesta debe ser sanitizado para prevenir Log Forging o XSS (aunque el serializador JSON de Spring maneja el escape básico).

### Edge Cases
*   **Estado Obsoleto (Stale State)**: Si extraes datos de `SecurityContextHolder`, ten en cuenta que el contexto podría estar parcialmente limpio o vacío si el error ocurrió muy temprano en la cadena de filtros.
*   **Nulabilidad**: `ex` puede ser nulo en algunos escenarios de error sintéticos.

### Ejemplos

**Enriquecedor Integrado (código real de la librería)**
```java
@Component
public class TraceIdEnricher implements ProblemDetailEnricher {
    @Override
    public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }
    
    @Override
    public int order() {
        return 100;
    }
}
```

---

## 3. `ErrorCode`

Enumeración canónica de los identificadores de error soportados.

### Firma Completa

```java
public enum ErrorCode {
  INTERNAL_ERROR, BAD_REQUEST, VALIDATION_ERROR, GONE, UNAUTHORIZED, 
  FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, 
  CONFLICT, TOO_MANY_REQUESTS, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT;

  public static ErrorCode fromStatus(int status);
  public String toKebabCase();
}
```

### Retorna y Contratos
*   **Shape**: `enum` de Java.
*   **Mutabilidad**: Estrictamente inmutable.
*   **Contratos**: Diseñado para proveer un vocabulario finito y legible por máquina para la clasificación de errores en toda la organización.

### Edge Cases
*   **Fallback de Estado (`fromStatus`)**: Si se pasa un estado HTTP no reconocido (ej. `418`), hace fallback a `BAD_REQUEST` si el estado es 4xx, y a `INTERNAL_ERROR` en caso contrario.

---

## 4. `ApiStandardProperties`

La clase `@ConfigurationProperties` que gobierna el comportamiento de la librería.

### Firma Completa

```java
@ConfigurationProperties(prefix = "api.standard")
public class ApiStandardProperties {
    public Errors getErrors();
    public DegradedMode getDegradedMode();
    public Redaction getRedaction();
}
```

*(Para una lista completa de parámetros de YAML/Properties, consulta la guía de [Instalación](installation.md)).*

### Lifecycle y Mutabilidad
*   **Lifecycle**: Instanciada una vez durante la inicialización del contexto de Spring Boot.
*   **Mutabilidad**: Los campos son mutables vía setters (requerido por Spring Boot), pero modificarlos programáticamente después del inicio es un **anti-patrón** y puede llevar a un comportamiento inconsistente entre hilos.
