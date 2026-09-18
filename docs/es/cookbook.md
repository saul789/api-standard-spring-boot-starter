# Cookbook: Recetas Prácticas 👨‍🍳

Todas las recetas de este cookbook se han tomado directamente del proyecto de ejemplo [sample-project](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project) incluido en este repositorio. Cada ejemplo de código ha sido verificado contra el código base real.

---

## El Concepto Clave: `ProblemDetailEnricher`

En lugar de modificar el código central del manejador de errores, la librería proporciona una interfaz simple llamada `ProblemDetailEnricher`.

Spring Boot detecta automáticamente cualquier clase en tu proyecto que implemente esta interfaz. Cuando ocurre un error, la librería le pasa el objeto `ProblemDetail` a tu clase, permitiéndote agregar campos personalizados (propiedades) antes de que el JSON sea enviado al cliente.

---

## Receta 1: Inyectar un Nombre de Aplicación Estático

**Escenario**: Tienes varios microservicios que utilizan esta librería y deseas que cada respuesta de error indique explícitamente qué microservicio generó el error.

### El Código

Crea una nueva clase en tu paquete de configuración:

```java
package com.example.config;

import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class AppNameEnricher implements ProblemDetailEnricher {

    @Override
    public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
        // Agregar un string estático identificando este microservicio
        problem.setProperty("service", "billing-api");
    }

    @Override
    public int order() {
        // 100 asegura que se ejecute después de los enriquecedores nativos de la librería.
        return 100; 
    }
}
```

### El Resultado

Cuando ocurra un error, tu JSON ahora incluirá automáticamente el campo `"service"`:

```json
{
  "type": "urn:problem-type:bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid parameters.",
  "traceId": "9b1deb4d-3b7d-4bad",
  "service": "billing-api" 
}
```

---

## Receta 2: Lanzar Excepciones de Negocio con ErrorCode

**Escenario**: Tu lógica de dominio detecta una violación de regla de negocio (ej. recurso duplicado, permisos insuficientes) y necesitas comunicarla limpiamente al cliente de la API.

*Origen: [ExceptionDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ExceptionDemoController.java)*

### El Código

```java
// Opción A: Usando el enum canónico ErrorCode
@GetMapping("/business-logic")
public void throwBusiness() {
    throw new BusinessException(
        ErrorCode.BAD_REQUEST, "Regla de negocio incumplida.", HttpStatus.BAD_REQUEST);
}

// Opción B: Usando ProblemException con generación automática de URN
@GetMapping("/problem-internal")
public void throwInternal() {
    // Generará automáticamente: urn:problem-type:too-many-requests
    throw new ProblemException("Límite de cuota alcanzado.", HttpStatus.TOO_MANY_REQUESTS);
}

// Opción C: Usando una URL de documentación personalizada en lugar de la URN auto-generada
@GetMapping("/problem-external")
public void throwExternal() {
    throw new ProblemException(
        "Error en la validación.",
        HttpStatus.BAD_REQUEST,
        "https://docs.saul.dev/errors/validation-guide");
}
```

### El Resultado

```json
{
  "type": "urn:problem-type:bad-request",
  "title": "Bad Request",
  "status": 400,
  "detail": "Regla de negocio incumplida.",
  "instance": "/api/demo/exceptions/business-logic",
  "code": "BAD_REQUEST",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "timestamp": "2026-05-19T13:34:00Z"
}
```

---

## Receta 3: i18n — Traducción Automática de Mensajes

**Escenario**: Tu API soporta múltiples idiomas. Quieres que los mensajes de error se traduzcan automáticamente según la cabecera `Accept-Language` del cliente, sin cambiar el código de tus controladores.

*Origen: [ExceptionDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ExceptionDemoController.java) + [messages.properties](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/resources/messages.properties)*

### El Código

**Paso 1**: Define tus claves en `messages.properties` y `messages_es.properties`:

```properties
# messages.properties (Inglés - por defecto)
sample.error.limit_reached=The project limit has been reached for user {0}.
error.user.already_exists=User with this email address is already registered.
```

```properties
# messages_es.properties (Español)
sample.error.limit_reached=Se ha alcanzado el límite del proyecto para el usuario {0}.
error.user.already_exists=El usuario con este correo ya se encuentra registrado.
```

**Paso 2**: Lanza la excepción usando la clave de i18n como mensaje:

```java
// La librería resolverá "sample.error.limit_reached" desde messages.properties
@GetMapping("/client-key")
public void throwClientKey() {
    throw new BusinessException(
        ErrorCode.FORBIDDEN, "sample.error.limit_reached", HttpStatus.FORBIDDEN);
}
```

### El Resultado

Con la cabecera `Accept-Language: es`:

```json
{
  "type": "urn:problem-type:forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Se ha alcanzado el límite del proyecto para el usuario {0}.",
  "code": "FORBIDDEN",
  "traceId": "..."
}
```

> **Nota**: Si la clave no se encuentra en los archivos `messages.properties`, el texto sin procesar se devuelve tal cual al cliente, lo que permite mensajes totalmente personalizados sobre la marcha sin archivos de propiedades.

---

## Receta 4: Validación de Beans con Detalles de Error Automáticos

**Escenario**: Quieres respuestas de error de validación estructuradas y detalladas cuando un cliente envía un JSON o parámetros de consulta no válidos.

*Origen: [ValidationDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ValidationDemoController.java) + [UserDto.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/dto/UserDto.java)*

### El Código

**DTO con anotaciones JSR-303:**
```java
public class UserDto {
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotNull(message = "Age is required")
    private Integer age;
}
```

**Controlador usando `@Valid`:**
```java
@PostMapping("/body")
public UserDto testBodyValidation(@Valid @RequestBody UserDto dto) {
    return dto;
}
```

### El Resultado

Envía una petición `POST /api/demo/validation/body` con el cuerpo `{"username": "", "age": null}`:

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "username: Username cannot be blank; age: Age is required",
  "code": "VALIDATION_ERROR",
  "traceId": "..."
}
```

> **Importante**: El motor de redacción (Redaction Engine) enmascarará automáticamente los valores de los campos que coincidan con los nombres sensibles configurados (ej. `password`, `token`, `secret`).

---

## Receta 5: Propagación de Errores de Clientes Feign

**Escenario**: El Microservicio A llama al Microservicio B a través de Feign. El Microservicio B falla. Necesitas que el error se propague limpiamente al consumidor original sin perder contexto.

*Origen: [FeignDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/FeignDemoController.java)*

### Cómo Funciona

La librería detecta automáticamente si `FeignException` está en el classpath e intercepta estos errores:

- **Errores de cliente (4xx)**: Se propagan exactamente tal cual (ej. un 404 se propaga como un 404).
- **Errores de servidor (5xx) o desconocidos**: Se transforman en un `502 Bad Gateway` estándar para evitar exponer detalles internos de la arquitectura.

### El Código

```java
// Simula un fallo de servicio ascendente 503 -> la librería devuelve un 502 Bad Gateway
@GetMapping("/upstream-failure")
public void throwFeignServiceUnavailable() {
    throw new feign.FeignException.ServiceUnavailable(
        "Upstream service down",
        feign.Request.create(
            feign.Request.HttpMethod.GET, "/external/api",
            java.util.Collections.emptyMap(), null, null, null),
        null, null);
}

// Simula un error 404 ascendente -> la librería lo propaga como un 404 Not Found
@GetMapping("/upstream-notfound")
public void throwFeignNotFound() {
    throw new feign.FeignException.NotFound(
        "Resource not found in remote service",
        feign.Request.create(
            feign.Request.HttpMethod.GET, "/remote-resource",
            java.util.Collections.emptyMap(), null, null, null),
        null, null);
}
```

---

## Receta 6: Manejo de Excepciones de Resilience4j

**Escenario**: Tu API utiliza Resilience4j para disyuntores (circuit breakers), limitadores de tasa (rate limiters) o mamparos (bulkheads). Cuando estos mecanismos se activan, necesitas respuestas RFC 9457 limpias en lugar de trazas de pila crudas.

*Origen: [ResilienceDemoController.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/controller/ResilienceDemoController.java)*

### Cómo Funciona

El `GlobalExceptionHandler` detecta y mapea de forma nativa las excepciones de Resilience4j:

| Excepción | Estado HTTP | Código de Error |
| :--- | :--- | :--- |
| `CallNotPermittedException` | `503 Service Unavailable` | `SERVICE_UNAVAILABLE` |
| `RequestNotPermitted` | `429 Too Many Requests` | `TOO_MANY_REQUESTS` |
| `BulkheadFullException` | `429 Too Many Requests` | `TOO_MANY_REQUESTS` |
| `TimeoutException` | `504 Gateway Timeout` | `GATEWAY_TIMEOUT` |

### El Código

```java
@GetMapping("/circuit-breaker")
public void circuitBreaker() {
    CircuitBreaker cb = CircuitBreaker.ofDefaults("demo-circuit");
    throw CallNotPermittedException.createCallNotPermittedException(cb);
}

@GetMapping("/rate-limiter")
public void rateLimiter() {
    RateLimiter limit = RateLimiter.of("demo-rate",
        RateLimiterConfig.custom()
            .limitForPeriod(1)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .build());
    throw RequestNotPermitted.createRequestNotPermitted(limit);
}
```

### El Resultado

```json
{
  "type": "urn:problem-type:too-many-requests",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "RequestNotPermitted: ...",
  "code": "TOO_MANY_REQUESTS",
  "traceId": "..."
}
```

---

## Receta 7: Excepción Personalizada con `@ResponseStatus`

**Escenario**: Tienes excepciones de dominio heredadas anotadas con `@ResponseStatus` y quieres que la librería las maneje automáticamente sin tener que modificarlas.

*Origen: [CustomConflictException.java](https://github.com/saul789/spring-boot-starter-api-standard/tree/main/sample-project/src/main/java/io/github/saul789/sample/exception/CustomConflictException.java)*

### El Código

```java
@ResponseStatus(HttpStatus.CONFLICT)
public class CustomConflictException extends RuntimeException {
    public CustomConflictException(String message) {
        super(message);
    }
}
```

Uso en un controlador:

```java
@GetMapping("/annotated")
public void throwAnnotated() {
    throw new CustomConflictException("Este recurso ya está siendo editado.");
}
```

### El Resultado

```json
{
  "type": "urn:problem-type:conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "Este recurso ya está siendo editado.",
  "code": "CONFLICT",
  "traceId": "..."
}
```

> La librería lee la anotación `@ResponseStatus` de tu clase de excepción y la utiliza automáticamente para establecer el código de estado HTTP y derivar la URN del campo `type`.
