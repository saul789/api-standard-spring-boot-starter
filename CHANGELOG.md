# Changelog

Todas las modificaciones notables de este proyecto serán documentadas en este archivo.

## [2.0.0] - 2026-05-15

### Añadido
- Arquitectura multi-módulo para mejor separación de responsabilidades:
  - `api-standard-core`: Contratos base, excepciones (`BusinessException`, `ProblemException`) y modelos. Sin dependencias de Spring Web.
  - `api-standard-web`: Adaptadores Web, filtros, manejadores de excepciones (`GlobalExceptionHandler`, `FeignExceptionHandler`, `ResilienceExceptionHandler`) y customizador de OpenAPI.
  - `api-standard-starter`: Módulo de auto-configuración que une todo.
- Archivo de reglas personalizado para PMD (`pmd-ruleset.xml`) para evadir fallas de parsing en Java 25 y falsos positivos.
- Propiedad `fmt.skip` en el `pom.xml` para permitir saltar el formateo en entornos con JDKs muy recientes (como Java 25) que rompen el plugin de Spotify.

### Cambiado
- Actualización a **Spring Boot 4.0.6**.
- Se renombró el artefacto raíz a `api-standard-spring-boot-starter` para seguir las convenciones de la comunidad.
- Incremento de la cobertura de pruebas unitarias a **más del 95%** en los módulos `core` y `web`.
- Refactorización de tests para remover dependencias circulares y necesidad de contexto de Spring en pruebas unitarias puras.

### Eliminado
- Directorio legado `starter/` en la raíz del proyecto.
- Anotaciones `@SuppressWarnings("PMD")` directas en el código, reemplazadas por exclusiones en el ruleset centralizado.
