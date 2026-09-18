# Preguntas Frecuentes (FAQ)

Esta sección cubre las preguntas más comunes de diferentes roles de ingeniería respecto a las decisiones arquitectónicas y el comportamiento operativo del `api-standard-spring-boot-starter`.

---

## Arquitectura y Diseño

### P: ¿Puedo excluir controladores específicos del manejador de errores global?
**R:** No. El `GlobalExceptionHandler` está anotado con `@RestControllerAdvice` sin restricción de `basePackages`, lo que significa que se aplica globalmente a todo el contexto de Spring.
Si necesitas un manejo de errores personalizado para un controlador específico, debes definir un método `@ExceptionHandler` *directamente dentro* de ese controlador. Spring le dará prioridad a tu manejador local sobre el advice global.

### P: ¿Por qué usamos RFC 9457 (`ProblemDetail`) en lugar de un formato JSON propio?
**R:** Para evitar la "deriva de contratos" (contract drift). En grandes ecosistemas empresariales, cada equipo tiende a inventar su propia estructura JSON de error (`{"error":...}`, `{"status":...}`, `{"message":...}`).
Al adoptar un estándar de la IETF (RFC 9457), los consumidores de la API (como aplicaciones Frontend o gateways externos) pueden escribir un único parser de errores universal que funcione para todos los microservicios.

---

## Rendimiento y SRE

### P: ¿Generar este JSON agrega latencia a la API?
**R:** La serialización del JSON (`ProblemDetailService`) y el pipeline de enriquecimiento tienen un overhead insignificante (complejidad $O(1)$).
El verdadero costo de rendimiento durante un error proviene de la JVM generando el stack trace cuando la excepción es lanzada (`throw`) inicialmente. Por lo tanto, nunca debes usar `BusinessException` para el flujo de control normal de tu aplicación.

### P: ¿Cómo correlaciono estas respuestas de error con mis logs en Datadog, ELK o Grafana?
**R:** El starter ejecuta automáticamente el `TraceIdEnricher`. Si existe un ID de traza en el Mapped Diagnostic Context (MDC) de SLF4J, será inyectado en el JSON de error bajo la llave `traceId`.
Para completar la correlación, asegúrate de que tu configuración de logs (`logback-spring.xml`) incluya la variable MDC (ej. `[%X{traceId}]`) para que tus logs coincidan con el JSON retornado al cliente.

---

## Seguridad

### P: ¿Cómo evita la librería filtrar datos sensibles (PII) en los errores de validación?
**R:** Cuando ocurre un `MethodArgumentNotValidException`, el starter expone los nombres de los campos que fallaron la validación. Para evitar filtrar datos sensibles (como un usuario enviando una contraseña inválida), la librería emplea una `DefaultRedactionPolicy`.
Por defecto, enmascara los valores de los campos llamados `password`, `token`, `secret`, `creditCard`, `cvv`, `ssn` y `authorization`.

### P: ¿Puedo cambiar qué campos son enmascarados por la política de ofuscación?
**R:** Sí. Puedes sobrescribir la lista por defecto en tu `application.yml`:

```yaml
api:
  standard:
    redaction:
      enabled: true
      mask: "***"
      fields:
        - "password"
        - "miTokenSecretoPersonalizado"
```
