# Guía de Contribución

Gracias por considerar contribuir al `api-standard-spring-boot-starter`. Esta librería es un componente de infraestructura core, lo que significa que la estabilidad y la compatibilidad hacia atrás son nuestras más altas prioridades.

---

## 1. Configuración Local

### Prerrequisitos
*   **Java 21** (LTS) o superior.
*   **Maven 3.8+**

### Arquitectura
El proyecto está dividido en tres módulos principales:
*   `api-standard-core`: Contiene las interfaces (`ProblemDetailEnricher`), `ErrorCode` y el `ProblemDetailService`. (Sin dependencias de Web/MVC).
*   `api-standard-web`: Contiene el `@RestControllerAdvice`, `GlobalExceptionHandler` y filtros. (Depende de Spring Web).
*   `api-standard-starter`: El módulo de auto-configuración. Contiene `spring.factories` / `AutoConfiguration.imports` que conectan todo cuando se agrega la dependencia.

### Compilación y Pruebas
Para compilar el proyecto y ejecutar todas las pruebas unitarias:
```bash
mvn clean install
```

---

## 2. Pruebas y Cobertura (Coverage)

Debido a su rol como una preocupación transversal (cross-cutting concern), requerimos una confiabilidad extremadamente alta.

*   **Cobertura Core del 100%**: Cualquier nuevo `Enricher` o modificación al `ProblemDetailService` debe estar acompañado de pruebas unitarias que cubran casos positivos, negativos y límites de nulos/edge cases.
*   **Sin Flaky Tests**: Las pruebas no deben depender de `Thread.sleep()` ni de llamadas de red externas.

---

## 3. Flujo de Release y Estándares

Este repositorio se adhiere estrictamente al versionamiento semántico automatizado.

### Conventional Commits
Todos los pull requests y commits deben seguir la especificación de [Conventional Commits](https://www.conventionalcommits.org/). Esto permite que nuestro pipeline de CI/CD genere automáticamente los changelogs y actualice las versiones.

**Ejemplos Válidos:**
*   `feat: add support for WebFlux exception handling` (Disparará un incremento MINOR: 2.x.0)
*   `fix: resolve NullPointerException in TraceIdEnricher` (Disparará un incremento PATCH: 2.0.x)
*   `feat!: completely redesign the ErrorCode enum` (El `!` indica un BREAKING CHANGE, disparando un incremento MAJOR: 3.0.0)

### Proceso de Pull Request
1.  Crea un feature branch desde `main`.
2.  Escribe tu código y agrega las pruebas necesarias.
3.  Asegúrate de que `mvn clean verify` pase exitosamente en local.
4.  Envía un Pull Request.
5.  Espera a que el pipeline de CI (GitHub Actions) pase la compilación y las barreras de calidad (Quality Gates) de SonarQube.
6.  Una vez aprobado por un mantenedor, será fusionado (Squash-Merged) en `main`.
