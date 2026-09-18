# Instalación y Configuración

Esta guía cubre cómo instalar el `api-standard-spring-boot-starter` y las opciones de configuración iniciales disponibles.

---

## Requisitos Mínimos

La librería ha sido verificada con el siguiente stack:

| Requisito | Versión | Notas |
| :--- | :--- | :--- |
| **Java** | 21 | Utiliza características como Pattern Matching y Records. |
| **Spring Boot** | 4.0.6 | Verificado con esta versión específica. |
| **Spring Web** | Incluido | Diseñado para aplicaciones Spring MVC. |

---

## Matriz de Compatibilidad

> [!WARNING]
> **Verificación Limitada**: El código fuente y la documentación actual del proyecto referencian explícitamente **Spring Boot 4.0.6** y **Java 21**. La compatibilidad con versiones anteriores (como Spring Boot 3.x o Java 17) u otros frameworks no ha sido verificada ni documentada en el repositorio.

---

## Instalación

Agrega la dependencia a tu sistema de build. Al ser un starter de Spring Boot, se auto-configurará una vez que esté presente en el classpath.

=== "Maven"

    Agrega lo siguiente a tu `pom.xml`:

    ```xml
    <dependency>
        <groupId>io.github.saul789</groupId>
        <artifactId>api-standard-spring-boot-starter</artifactId>
        <version>2.0.0</version>
    </dependency>
    ```

=== "Gradle"

    Agrega lo siguiente a tu `build.gradle`:

    ```groovy
    implementation 'io.github.saul789:api-standard-spring-boot-starter:2.0.0'
    ```

---

## Configuración Inicial

Puedes personalizar el comportamiento del starter agregando las siguientes propiedades a tu `application.yml` o `application.properties`.

Aquí están todas las propiedades disponibles en el código fuente (`ApiStandardProperties.java`):

```yaml
api:
  standard:
    errors:
      # URI base para el campo 'type' en las respuestas ProblemDetail.
      type-base-uri: "urn:problem-type:"
      
      # Mapa de códigos de error a URLs de documentación personalizadas.
      type-overrides:
        # Ejemplo:
        # NOT_FOUND: "https://docs.example.com/errors/not-found"
        
    degraded-mode:
      # Si se debe habilitar el filtro y el enriquecedor de modo degradado.
      enabled: true
      
      # Umbral de porcentaje de uso de CPU para activar el modo degradado.
      cpu-threshold: 80.0
      
      # Si se debe mostrar el flag 'degraded' en la respuesta JSON.
      show-in-json: false
      
      # Si se debe mostrar la cabecera 'X-API-Degraded' en la respuesta.
      show-in-header: true
      
    redaction:
      # Si se debe habilitar el motor de ofuscación de PII.
      enabled: true
      
      # La máscara para reemplazar los valores de los campos sensibles.
      mask: "[REDACTED]"
      
      # Lista de nombres de campos cuyos valores deben ser ofuscados.
      fields:
        - "password"
        - "token"
        - "secret"
        - "creditCard"
        - "cvv"
        - "ssn"
        - "authorization"
```

---

## Ecosistema JS y Compatibilidad de Plataforma (SSR/ESM/CJS)

> [!NOTE]
> **No Aplica**: Esta es una librería de lado del servidor para Java/Spring Boot. Los sistemas de módulos de JavaScript (ESM, CJS) y los paradigmas de Renderizado del Lado del Servidor (SSR) no aplican aquí.
> 
> **Soporte para WebFlux**: El soporte para Spring WebFlux no está implementado ni probado explícitamente en el código base actual (los handlers actuales se basan en Spring MVC y `HttpServletRequest`).

---

## Problemas Comunes y Solución de Problemas

### Problema: Dependencia Circular
*   **Síntoma**: Spring falla al iniciar con un error de referencia circular que involucra a `GlobalExceptionHandler` o filtros de seguridad.
*   **Causa Raíz**: Configuraciones de seguridad personalizadas que inyectan el manejador de excepciones demasiado pronto.
*   **Solución**: Asegúrate de que tu configuración de seguridad no dependa ansiosamente de los beans de manejo de errores o ajusta el `@Order` de tus filtros.

### Problema: Carga de CPU No Disponible
*   **Síntoma**: El modo degradado está habilitado pero nunca se activa ni registra advertencias.
*   **Causa Raíz**: El sistema operativo subyacente o la JVM no soportan `OperatingSystemMXBean.getCpuLoad()` (retorna -1).
*   **Solución**: En estos entornos, la librería asume por defecto una carga normal. Se requiere una implementación personalizada de `SystemLoadMonitor` si necesitas usar otra métrica.
