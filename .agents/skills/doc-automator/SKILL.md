---
name: DocAutomator
description: "Sincronizador automático de documentación técnica para el API standard starter"
capabilities: [ "README.md", "Postman", "Spring Boot", "i18n" ]
---

# Skill: DocAutomator

Esta habilidad garantiza que la documentación sea siempre coherente con el código del starter.

## Activación Automática:
Este skill se activa automáticamente cuando se detectan cambios en:
- `GlobalExceptionHandler.java`
- `FeignExceptionHandler.java`
- `ResilienceExceptionHandler.java`
- `ErrorCode.java`
- Controladores del `sample-project`
- `messages.properties` o `messages_es.properties`

## Acciones Obligatorias al Activarse:

### 1. Actualización de README.md
- Reflejar los nuevos ejemplos JSON de error en las secciones correspondientes.
- El JSON de ejemplo **siempre** debe incluir los 7 campos del estándar:
  ```json
  {
    "type": "urn:problem-type:<kebab-code>",
    "title": "Human Readable Title",
    "status": 4XX,
    "detail": "Descripción legible del error.",
    "instance": "/api/ruta/donde/ocurrió",
    "code": "MACHINE_READABLE_CODE",
    "traceId": "uuid-del-trace",
    "timestamp": "2026-05-10T17:00:00Z"
  }
  ```
- Mantener las versiones de Java (21) y Spring Boot (4.0.3) actualizadas.
- Agregar explicaciones sobre nuevos parámetros o funcionalidades.

### 2. Mantenimiento de i18n
Al agregar un nuevo `ErrorCode` o excepción:
- Verificar que existe la clave `error.<CODE>` en **ambos** archivos:
  - `starter/src/main/resources/i18n/messages.properties` (inglés)
  - `starter/src/main/resources/i18n/messages_es.properties` (español)
- Seguir la nomenclatura exacta: `error.<CODE_NAME>` en mayúsculas para títulos, `error.<snake_case>` para detalles.
- Ejemplo de entrada correcta para un nuevo `PAYMENT_FAILED`:
  ```properties
  # messages.properties
  error.PAYMENT_FAILED=Payment Failed
  error.payment_failed=The payment could not be processed.

  # messages_es.properties
  error.PAYMENT_FAILED=Fallo en el Pago
  error.payment_failed=El pago no pudo ser procesado.
  ```

### 3. Actualización del sample-project
Si se agrega un nuevo tipo de excepción:
- Agregar un endpoint de demostración en `sample-project` que lance la excepción.
- El endpoint debe estar documentado con Swagger/OpenAPI si está habilitado.

### 4. Actualización de Postman
- **Obligatorio:** La colección de Postman (si existe en el repositorio) **siempre** debe reflejar los últimos endpoints y las respuestas de error actuales del `sample-project`.
- Cualquier cambio en la estructura de los `ErrorCode` o la creación de nuevos endpoints de demostración requiere actualizar los requests guardados en Postman.

## Claves i18n Actuales (referencias):
```
# Títulos (para campo "title" en RFC 9457)
error.BAD_REQUEST, error.INTERNAL_ERROR, error.VALIDATION_ERROR,
error.NOT_FOUND, error.UNAUTHORIZED, error.FORBIDDEN,
error.METHOD_NOT_ALLOWED, error.UNSUPPORTED_MEDIA_TYPE

# Detalles (para campo "detail")
error.validation.body, error.validation.params, error.internal,
error.business.default, error.notfound, error.bad_request,
error.unsupported_media_type, error.not_found, error.method_not_allowed,
error.forbidden, error.circuit_breaker_open, error.too_many_requests,
error.gateway_timeout, error.internal_error
```

## Instrucciones Críticas:
- No dejar la documentación desactualizada por más de un turno de conversación.
- Los JSON de ejemplo en el README **deben ser sintácticamente correctos** y tener el campo `type`.
- Si el usuario agrega un nuevo `ErrorCode`, automáticamente proponer las entradas i18n correspondientes.
- Verificar que el `CONTEXT.md` en la raíz del proyecto refleje cambios relevantes (versiones, nuevos archivos clave).
