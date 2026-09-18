# Solución de Problemas (Troubleshooting)

Esta guía te ayuda a diagnosticar y solucionar los problemas más comunes que podrías encontrar al integrar el `api-standard-spring-boot-starter`.

---

## Tabla de Diagnóstico

| Síntoma | Causa Raíz Probable | Solución |
| :--- | :--- | :--- |
| **Falta el `traceId` en el JSON** | El MDC de SLF4J está vacío cuando ocurre el error. | Asegúrate de tener instalada una librería de tracing (como Micrometer Tracing) o puebla manualmente el `MDC.put("traceId", ...)` en un filtro anterior. Además, verifica que la excepción no haya ocurrido en un hilo en segundo plano (background thread) que perdió el contexto MDC. |
| **La respuesta es HTML en lugar de JSON** | El cliente está solicitando HTML, o usaste `@Controller` en lugar de `@RestController`. | Asegúrate de que el cliente envíe `Accept: application/json`. Verifica que tus controladores estén anotados con `@RestController`. |
| **Un `ProblemDetailEnricher` personalizado no se ejecuta** | Spring Boot no detectó tu bean. | Verifica que tu clase enriquecedora esté anotada con `@Component` y se encuentre dentro de un paquete cubierto por el component scan de tu `@SpringBootApplication`. |
| **`BusinessException` retorna HTTP 500 en lugar de 4xx** | La excepción fue capturada y envuelta por un proxy o límite de transacción. | Los proxies de `@Transactional` de Spring pueden envolver excepciones comprobadas (checked exceptions) en una `UndeclaredThrowableException`. Dado que `BusinessException` es un `RuntimeException`, asegúrate de que ningún aspecto (AspectJ) intermedio la esté tragando y relanzando de forma genérica. |
| **El modo degradado (`X-API-Degraded`) nunca se activa** | El sistema operativo subyacente o la JVM no exponen métricas de CPU. | Si `OperatingSystemMXBean.getCpuLoad()` retorna `-1`, la librería asume por defecto "carga normal". Debes implementar un `SystemLoadMonitor` personalizado si necesitas usar una métrica diferente (como memoria o hilos activos). |
| **Los errores de validación no muestran los campos específicos** | La excepción lanzada no fue `MethodArgumentNotValidException`. | Asegúrate de estar usando `@Valid` o `@Validated` en los parámetros del payload de tu controlador, en lugar de lanzar excepciones genéricas manualmente. |

---

## Depuración Profunda (Debugging)

Si la tabla anterior no resuelve tu problema, puedes habilitar los logs de traza (trace logs) para ver exactamente cómo la librería está evaluando las excepciones.

### Habilitar Logs de Traza

Agrega lo siguiente a tu `application.yml` (solo en desarrollo):

```yaml
logging:
  level:
    io.github.saul789.api.standard: TRACE
```

Esto registrará en consola:
*   Cada vez que el `GlobalExceptionHandler` intercepta un error.
*   El `ErrorCode` exacto que fue resuelto.
*   Fallas silenciosas (como errores al intentar resolver URIs personalizadas o rutas de la petición).
