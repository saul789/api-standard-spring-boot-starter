# Inicio Rápido (Quick Start)

Esta guía te ayudará a crear un proyecto mínimo realista para ver el `api-standard-spring-boot-starter` en acción.

---

## 1. Estructura del Proyecto

Un proyecto típico de Spring Boot utilizando este starter se ve así:

```text
mi-api/
├── pom.xml (o build.gradle)
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

## 2. Implementación

Utilizaremos la excepción nativa `BusinessException` y el `ErrorCode` provistos por la librería para simular una violación de regla de negocio (por ejemplo, intentar crear un recurso que ya existe).

Crea el controlador en `DemoController.java`:

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
        // Explicación línea por línea:
        // 1. Lanzamos una BusinessException provista por el starter.
        // 2. Pasamos el ErrorCode.CONFLICT canónico (mapea a 409).
        // 3. Proporcionamos un mensaje claro y amigable para el usuario.
        // 4. Establecemos el estado HTTP a CONFLICT (409).
        throw new BusinessException(
            ErrorCode.CONFLICT, 
            "El recurso solicitado ya está en uso.",
            HttpStatus.CONFLICT
        );
    }
}
```

---

## 3. Flujo en Runtime

Cuando un cliente realiza una petición a `/api/test-error`, la siguiente secuencia ocurre automáticamente sin necesidad de código extra:

1.  **Excepción Lanzada**: El `DemoController` lanza `BusinessException`.
2.  **Intercepción**: El `GlobalExceptionHandler` de la librería (registrado con la menor precedencia) captura la excepción.
3.  **Procesamiento**: El `ProblemDetailService` comienza a construir la respuesta bajo el estándar RFC 9457.
4.  **Pipeline de Enriquecimiento**: La librería ejecuta los enriquecedores activos presentes en el código:
    *   `StandardMetadataEnricher`: Agrega el `code` legible por máquina y el `timestamp`.
    *   `TraceIdEnricher`: Extrae el ID de traza del MDC de logs e lo inyecta.
5.  **Respuesta**: Se serializa un JSON `ProblemDetail` y se retorna al cliente.

---

## 4. Resultado Esperado

Realiza una petición al endpoint (por ejemplo, usando `curl`). El starter interceptará la excepción y devolverá un JSON limpio y estructurado que cumple con RFC 9457:

```json
{
  "type": "urn:problem-type:conflict",
  "title": "Conflict",
  "status": 409,
  "detail": "El recurso solicitado ya está en uso.",
  "instance": "/api/test-error",
  "code": "CONFLICT",
  "traceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "timestamp": "2026-05-18T21:34:00Z"
}
```

Esto garantiza que todos los errores en tu aplicación sigan exactamente el mismo contrato.
