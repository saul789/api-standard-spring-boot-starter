---
name: ProjectWorkflow
description: "Flujo de trabajo estándar y reglas de operación para modificaciones en el spring-boot-starter-api-standard"
---

# Skill: ProjectWorkflow

Esta habilidad define el proceso de trabajo que debo seguir en **cada tarea** de este proyecto, sin que el usuario tenga que recordármelo.

## 🔍 Paso 0: Contexto Obligatorio (Antes de cualquier cambio)

Antes de escribir código, siempre leer:
1. `CONTEXT.md` — Arquitectura, decisiones y restricciones del proyecto
2. `ROADMAP.md` — Para entender el estado actual y qué feature está activa
3. El archivo que voy a modificar — Nunca editar a ciegas

## 🔄 Flujo Obligatorio por Tipo de Tarea

### Al agregar una nueva excepción o ErrorCode:
```
1. Agregar el nuevo valor en ErrorCode.java (con Javadoc)
2. Agregar la entrada en messages.properties (inglés)
3. Agregar la entrada en messages_es.properties (español)
4. Agregar el @ExceptionHandler en GlobalExceptionHandler.java (si aplica)
5. Agregar test en GlobalExceptionHandlerTest.java
6. Verificar cobertura: mvn clean verify
7. [DocAutomator] Actualizar README.md con el nuevo error
```

### Al modificar GlobalExceptionHandler.java:
```
1. Leer CONTEXT.md → sección "Decisiones de Arquitectura"
2. Verificar que el contrato RFC 9457 no se rompe
3. Correr tests existentes ANTES de modificar: mvn test
4. Hacer el cambio
5. Correr: mvn clean verify (build completo con cobertura)
6. [DocAutomator] Actualizar ejemplos JSON en README si el formato cambió
```

### Al agregar un ProblemDetailEnricher nuevo:
```
1. Crear clase que implemente ProblemDetailEnricher (NO modificar ProblemDetailService)
2. Registrar el bean en ApiStandardAutoConfiguration.java
3. Crear test unitario para el nuevo enricher
4. mvn clean verify
```

### Al actualizar la versión del proyecto:

> ⚠️ **REGLA CRÍTICA:** `starter/` y `sample-project/` tienen ciclos de vida **independientes**.
> - Cambio solo en `starter/` → actualizar version SOLO en el root pom + starter. **NUNCA tocar sample-project.**
> - Cambio solo en `sample-project/` → actualizar solo ese módulo. **NUNCA tocar la versión del starter.**
> - Cambio en ambos → actualizar coordinadamente y documentarlo en el commit.

```
1. Identificar qué módulo cambia: ¿starter/, sample-project/, o ambos?
2. Actualizar SOLO la versión del módulo afectado en su pom.xml
3. Si es el starter → actualizar también el root pom.xml (parent)
4. Actualizar la tabla de versiones en CONTEXT.md
5. Actualizar la tabla de versiones en ROADMAP.md
6. Hacer tag en Git: git tag v<versión>   (solo para el starter, que es la lib publicada)
```

#### Tabla de decisión de versión (SemVer):

| Tipo de cambio en `starter/` | Versión |
|---|---|
| Solo `fix` / `perf` | PATCH → `1.2.0` → `1.2.1` |
| `feat` (compatible) | MINOR → `1.2.0` → `1.3.0` |
| Breaking change en API pública | MAJOR → `1.2.0` → `2.0.0` |
| Solo `docs`, `test`, `chore` | Sin cambio de versión |

#### Qué es breaking change en este proyecto:
- Cambiar firma de `BusinessException`, `ProblemException`, `ProblemDetailEnricher`, `ProblemTypeProvider`, `@ProblemType`
- Eliminar un valor de `ErrorCode`
- Renombrar/eliminar propiedades de `ApiStandardProperties`
- Cambiar campos del JSON de respuesta de error (`code`, `traceId`, `timestamp`)

## ⚡ Comandos de Referencia Rápida

```bash
# ✅ Comando principal (usar antes de cualquier PR)
mvn clean verify

# 🧪 Solo tests (rápido, para desarrollo)
mvn test

# 📊 Ver reporte de cobertura (abrir en browser)
# Windows: start starter/target/site/jacoco/index.html

# 🔍 Análisis estático SpotBugs
mvn spotbugs:check

# 📦 Instalar localmente (para probar en sample-project)
mvn clean install -DskipTests

# 🚀 Build sin SpotBugs (solo para desarrollo rápido)
mvn clean verify -Dspotbugs.skip=true
```

## 📁 Estructura de Paquetes (Referencia)

```
starter/src/main/java/io/github/saul789/api/standard/
├── ApiStandardAutoConfiguration.java  ← Registra todos los beans
├── ApiStandardProperties.java         ← Propiedades configurables
├── advice/
│   └── ApiResponseAdvice.java         ← Wrapping de respuestas exitosas
├── exception/
│   ├── BusinessException.java         ← ⭐ Excepción principal para devs
│   ├── ProblemException.java          ← Base de BusinessException
│   ├── ErrorCode.java                 ← ⭐ Enum de códigos de error
│   ├── GlobalExceptionHandler.java    ← ⭐ Manejo centralizado
│   ├── FeignExceptionHandler.java     ← Errores de clientes Feign
│   ├── ResilienceExceptionHandler.java ← Resilience4j
│   ├── ProblemDetailService.java      ← Construcción del ProblemDetail
│   ├── ProblemDetailEnricher.java     ← Interfaz de enriquecimiento
│   ├── StandardMetadataEnricher.java  ← Agrega code, timestamp
│   ├── TraceIdEnricher.java           ← Agrega traceId desde MDC
│   ├── ProblemType.java               ← Anotación @ProblemType
│   └── ProblemTypeProvider.java       ← Interfaz para tipo custom
├── filter/
│   ├── RequestLoggingFilter.java      ← Log de cada request
│   └── TraceContextFilter.java        ← Propaga traceId al MDC
└── model/
    └── ValidationError.java           ← DTO para errores de validación
```

## 🚨 Reglas de Calidad (Nunca ignorar)

| Regla | Umbral | Dónde se configura |
|---|---|---|
| Cobertura JaCoCo | ≥ 95% instructions | `pom.xml` raíz → `check-coverage` execution |
| SpotBugs | Threshold: Low / Effort: Max | `pom.xml` raíz → `spotbugs-maven-plugin` |
| Javadoc | Obligatorio en clases y métodos públicos nuevos | Revisión manual |
| i18n | Cada ErrorCode debe tener clave en ambos idiomas | Revisión manual |

## 📝 Conventional Commits (Mensajes de Commit Obligatorios)

**Spec:** https://www.conventionalcommits.org/en/v1.0.0/

Todo commit en este proyecto **debe** seguir el formato:
```
<type>(<scope>): <descripción corta en imperativo>
```

### Tipos y scopes válidos

| Type | Cuándo | Scopes habituales |
|---|---|---|
| `feat` | Nueva funcionalidad | `exception`, `handler`, `service`, `advice`, `filter`, `config` |
| `fix` | Bug fix | `exception`, `handler`, `service` |
| `test` | Tests nuevos/corregidos | `exception`, `handler`, `advice`, `filter` |
| `docs` | Documentación | `docs`, `readme`, `context` |
| `refactor` | Refactor sin cambio de comportamiento | `service`, `handler`, `exception` |
| `chore` | Mantenimiento | `deps`, `pom`, `config` |
| `ci` | GitHub Actions | `github`, `workflow` |
| `build` | Sistema de build | `pom`, `jacoco`, `spotbugs` |
| `perf` | Rendimiento | cualquiera |
| `style` | Formato sin lógica | cualquiera |

> ⚠️ **BREAKING CHANGE:** Usar `!` después del type: `feat(exception)!: ...`
> Esto sube la versión **major** (`1.2.0` → `2.0.0`).

### Ejemplos para este proyecto

```bash
feat(exception): add REQUEST_TIMEOUT error code with 408 mapping
fix(handler): BusinessException custom URL ignored when type-override configured
test(exception): add edge cases for ResilienceExceptionHandler bulkhead
docs(context): add Conventional Commits standard to CONTEXT.md
chore(deps): upgrade Spring Boot to 4.0.3
refactor(service): extract resolveType to dedicated private method
ci(github): add coverage gate to publish workflow
feat(exception)!: rename ProblemException param url to typeUri
```

### Regla crítica para el agente:
- Cuando el usuario pida hacer un commit, **siempre** proponer el mensaje siguiendo este estándar.
- Si el cambio incluye `BREAKING CHANGE`, advertir explícitamente al usuario antes de proceder.

---

## 🤖 Cómo Informar al Usuario

Al terminar una tarea, siempre reportar:
1. ✅ Qué se hizo
2. 📁 Qué archivos se modificaron
3. 🧪 Estado de tests (si se corrieron)
4. ⚠️ Qué sigue (próximo paso del ROADMAP si aplica)
5. ❓ Si hay decisiones pendientes del usuario
6. 💬 Mensaje de commit sugerido (Conventional Commits)

## 💬 Cómo Pedir Tareas (Guía para el usuario)

El agente trabaja mejor cuando el usuario especifica:

| En vez de... | Di... |
|---|---|
| "Agrega manejo de errores" | "Agrega soporte para `PaymentException` con código `PAYMENT_FAILED` y HTTP 402, siguiendo el patrón de `BusinessException`" |
| "Arregla el test" | "El test `shouldHandleXxx` falla en el assertion del campo `type`, revísalo" |
| "Actualiza la doc" | "Usa la skill DocAutomator para sincronizar README con los cambios de este PR" |
| "Hay un bug" | "Al lanzar `BusinessException` con URL custom, el campo `type` en la respuesta sigue siendo la URN por defecto" |
