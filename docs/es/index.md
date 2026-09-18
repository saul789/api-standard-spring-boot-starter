# API Standard Starter

Estandarización de APIs e inteligencia de errores de nivel enterprise para Spring Boot.

`api-standard-spring-boot-starter` es una librería diseñada para estandarizar el manejo de errores en aplicaciones Spring Boot, forzando el cumplimiento estricto del estándar **RFC 9457 (Problem Details for HTTP APIs)**.

Proporciona un contrato unificado para las respuestas de error, ofuscación automática de datos sensibles (PII) y un indicador pasivo de carga del sistema.

---

## El Problema que Resuelve

En sistemas distribuidos y microservicios, las respuestas de error inconsistentes provocan:
*   **Deriva de Contratos**: Diferentes endpoints o servicios devolviendo estructuras JSON distintas para errores similares.
*   **Brechas de Trazabilidad**: Errores que carecen de un ID de correlación unificado, dificultando la agregación de logs y la depuración.
*   **Fuga de Información**: Exposición accidental de stack traces internos o detalles de la base de datos en las respuestas de producción.

Este starter proporciona un mecanismo centralizado para capturar excepciones y renderizarlas en un formato predecible y legible por máquinas.

---

## Capacidades Core

La librería proporciona las siguientes características basadas en el código fuente actual:

*   **Cumplimiento RFC 9457**: Todos los errores manejados retornan un cuerpo `ProblemDetail` con los campos estándar (`type`, `title`, `status`, `detail`, `instance`).
*   **Códigos de Error**: Mapea excepciones a un conjunto de códigos de error canónicos (ej. `INTERNAL_ERROR`, `VALIDATION_ERROR`, `NOT_FOUND`).
*   **Inyección de Trace ID**: Inyecta automáticamente un `traceId` extraído del MDC en la respuesta de error.
*   **Ofuscación de PII**: Enmascara campos sensibles (como `password`, `creditCard`, `token`) en los detalles de los errores de validación.
*   **Indicador de Modo Degradado**: Agrega una cabecera o flag en el JSON cuando el uso de CPU del sistema excede un umbral configurable.

---

## Arquitectura Resumida

La librería opera como una capa no intrusiva sobre Spring MVC:

1.  **Intercepción**: Un `@ControllerAdvice` (`GlobalExceptionHandler`) intercepta las excepciones lanzadas.
2.  **Enriquecimiento**: Un pipeline de beans `ProblemDetailEnricher` (como `StandardMetadataEnricher` y `TraceIdEnricher`) agrega metadatos operativos a la respuesta.
3.  **Serialización**: El `ProblemDetail` final se serializa como JSON.

---

## Benchmarks y Casos de Producción

> [!NOTE]
> Métricas específicas de benchmark (latencia de overhead, capacidad de throughput) y casos de estudio de empresas específicas no están disponibles en el código fuente ni en la documentación actual del proyecto. La librería está diseñada para ser ligera minimizando alocaciones de objetos en el path de manejo de excepciones, pero no se declaran números oficiales de rendimiento.

---

## Ejemplo Rápido

Cuando una validación falla, la librería produce una respuesta como esta:

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

## Próximos Pasos

*   **[Instalación](installation.md)**: Agrega la dependencia a tu proyecto.
*   **[Inicio Rápido](quickstart.md)**: Mira cómo lanzar y manejar un error estándar.
