# 🛡️ Security Governance & Patch Policy

Este documento define la estrategia de gestión de vulnerabilidades para el `spring-boot-starter-api-standard`. El objetivo es mantener una postura de seguridad proactiva, priorizando la remediación técnica sobre la supresión de alertas.

## ⚖️ Principios de Seguridad

1.  **Parchear primero (Patch First)**: Ante una vulnerabilidad (CVE), la primera opción es siempre actualizar la versión de la dependencia en el BOM o vía `dependencyManagement`.
2.  **Supresión como último recurso (Suppress Last)**: Solo se permite el uso de `suppressions.xml` si:
    *   Es un falso positivo confirmado.
    *   La vulnerabilidad no es explotable en el contexto de una librería (ej. vulnerabilidades de despliegue en una lib).
    *   No existe parche disponible y el riesgo es aceptable/bajo.
3.  **Independencia de Framework**: Aprovechar las actualizaciones de Spring Boot para heredar parches transitivos.

---

## 📅 Registro de Operaciones (Patch Log)

### [2026-05-10] Upgrade a Spring Boot 4.0.6 & Cleanup

**Objetivo:** Reducir la superficie de ataque eliminando supresiones manuales y actualizando el core del starter.

| Librería | Acción | Motivo |
| :--- | :--- | :--- |
| **Spring Boot** | Upgrade `4.0.3` → `4.0.6` | Heredar parches críticos en `spring-core`, `spring-security` y `tomcat`. |
| **Jackson** | Remoción de v2.x | Conflicto con la v3.x nativa de Spring Boot 4; elimina alertas de incompatibilidad. |
| **Commons-FileUpload**| Remoción total | Librería vulnerable y obsoleta (Java EE). No tiene uso real en el código. |
| **Lombok** | Upgrade a `1.18.36` | Compatibilidad mejorada con JDK 21 y Spring Boot 4. |

### 🛠️ Vulnerabilidades Remanentes (Supresiones Activas)

Actualmente, solo se mantienen supresiones para:
*   **Swagger-UI / DOMPurify**: Vulnerabilidades de mXSS en el bundle de Javascript que no afectan al backend de Java y son necesarias para la documentación interactiva en entornos de desarrollo.

---

## 🔍 Guía para el Desarrollador

Cuando el pipeline de CI falle por `dependency-check`:

1.  **Analizar el reporte HTML**: Identificar si la dependencia es directa o transitiva.
2.  **Intentar Update**: Buscar si existe una versión superior en Maven Central.
3.  **Aplicar Parche**:
    ```xml
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.vulnerable</groupId>
                <artifactId>library</artifactId>
                <version>versión-parcheada</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
    ```
4.  **Si no hay parche**: Documentar el análisis en este archivo y agregar la supresión con una nota clara en `dependency-check-suppressions.xml`.
