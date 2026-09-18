# Guía de Migración

Esta guía te ayuda a hacer la transición desde el manejo de errores por defecto (vanilla) de Spring Boot hacia el formato estructurado y compatible con RFC 9457 del `api-standard-spring-boot-starter`.

---

## 1. El Cambio Estructural (Breaking Change)

El cambio más significativo es la estructura del JSON que se retorna a tus clientes.

**Antes (Spring Boot por defecto)**
```json
{
  "timestamp": "2026-05-18T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/resource"
}
```

**Ahora (API Standard Starter / RFC 9457)**
```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/resource",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-05-18T10:00:00Z"
}
```

**Impacto en el Frontend**: Cualquier aplicación frontend, app móvil o API gateway que analice los mensajes de error debe actualizarse para leer la propiedad `detail` en lugar de `message`, y `title` en lugar de `error`.

---

## 2. Checklist de Migración

### Paso 1: Eliminar Manejadores Globales Personalizados
Si tu proyecto actualmente tiene una clase `@ControllerAdvice` o `@RestControllerAdvice` que captura `Exception.class` o `RuntimeException.class`, debes eliminarla o restringir su alcance. De lo contrario, competirá con el `GlobalExceptionHandler` del starter.

### Paso 2: Adoptar `BusinessException`
Encuentra tus excepciones de dominio personalizadas (ej. `UsuarioNoEncontradoException`, `PagoInvalidoException`).
Refactorízalas para que extiendan de `BusinessException` directamente, o mapéalas a los valores canónicos de `ErrorCode` provistos por la librería.

### Paso 3: Actualizar Tests de Integración
Si tienes pruebas de integración (`@SpringBootTest` con `MockMvc`) que hacen aserciones sobre rutas JSON específicas (como `$.message`), debes actualizarlas:
*   Cambiar `$.message` a `$.detail`.
*   Cambiar `$.error` a `$.title`.
*   Cambiar `$.path` a `$.instance`.

---

## 3. Estrategia de Despliegue Gradual

Si tienes una arquitectura grande de microservicios, no intentes una migración "big bang" (todo a la vez).

1.  **Fase 1: Parser Dual en el Frontend**. Actualiza tus interceptores HTTP del Frontend (ej. Axios, Fetch) para manejar *ambos* formatos con gracia. Revisa la existencia de la llave `type` o `detail` para determinar si la respuesta es RFC 9457.
2.  **Fase 2: Servicios Opt-in**. Agrega la dependencia del starter primero a microservicios no críticos o de nueva creación.
3.  **Fase 3: Servicios Core**. Migra tus servicios principales y monolitos legados.
4.  **Fase 4: Limpieza**. Elimina los parsers antiguos (legacy) del frontend.
