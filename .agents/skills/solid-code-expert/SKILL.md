---
name: SolidCodeExpert
description: "Habilidad para garantizar código de alta calidad, siguiendo principios SOLID y requerimientos de cobertura de tests"
---

# Skill: SolidCodeExpert

Esta habilidad me obliga a aplicar los principios SOLID y mantener una estrategia de testing rigurosa en cada modificación del código.

## Principios SOLID a seguir:
1. **Single Responsibility (S)**: Cada clase debe tener una única razón para cambiar. Por ejemplo, el `GlobalExceptionHandler` solo mapea excepciones; `ProblemDetailService` construye el ProblemDetail; los `ProblemDetailEnricher` enriquecen. No mezclar responsabilidades.
2. **Open/Closed (O)**: Para agregar lógica de enriquecimiento, crear un nuevo `ProblemDetailEnricher`, NO modificar `ProblemDetailService`. Extender, no modificar.
3. **Liskov Substitution (L)**: Las subclases de `ProblemException` (como `BusinessException`) deben poder usarse indistintamente en el `GlobalExceptionHandler` sin romper el contrato RFC 9457.
4. **Interface Segregation (I)**: `ProblemDetailEnricher` es pequeña y específica. `ProblemTypeProvider` es una interfaz de un solo método. No crear interfaces monolíticas.
5. **Dependency Inversion (D)**: Inyectar `MessageSource` y `ApiStandardProperties` siempre, nunca instanciar directamente. Los `ProblemDetailEnricher` se inyectan como lista.

## Requerimientos de Cobertura (Coverage):
La meta de cobertura es **95% de instructions** (enforced por JaCoCo en `pom.xml`). No negociable.

Siempre que se añada una funcionalidad o se refactorice código:
1. **Unit Testing Obligatorio**: Cada clase nueva debe tener su correspondiente test unitario en `starter/src/test/java`.
2. **Meta de Cobertura**: **95% mínimo** de instruction coverage en el módulo `starter`. Si el build falla por cobertura, es un blocker.
3. **Escenarios Edge-Case**: Los tests deben cubrir:
   - El camino feliz (happy path)
   - Null inputs y parámetros vacíos
   - Internacionalización (Locale.ENGLISH, Locale("es"))
   - Excepciones con y sin causa raíz
   - Campos opcionales presentes y ausentes
4. **Pruebas de Regresión**: Al modificar el Handler, siempre correr los tests existentes para asegurar que el estándar RFC 9457 sigue intacto.

## Patrón estándar de setUp() para tests:

```java
@BeforeEach
void setUp() {
    this.messageSource = mock(MessageSource.class);
    // El mock siempre devuelve el tercer argumento (fallback) para simplificar
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
            .thenAnswer(invocation -> invocation.getArgument(2));

    this.properties = new ApiStandardProperties();
    List<ProblemDetailEnricher> enrichers = List.of(
            new StandardMetadataEnricher(),
            new TraceIdEnricher());
    this.problemDetailService = new ProblemDetailService(messageSource, properties, enrichers);
    this.handler = new GlobalExceptionHandler(messageSource, problemDetailService);
    this.request = mock(HttpServletRequest.class);
}

@AfterEach
void tearDown() {
    MDC.clear(); // Siempre limpiar MDC para no contaminar otros tests
}
```

## Qué verificar siempre en tests de error (campos RFC 9457):

```java
// Obligatorio en TODOS los tests de error:
assertEquals(HttpStatus.XXX.value(), result.getStatus());
assertNotNull(result.getType());                                  // nunca null
assertEquals("EXPECTED_CODE", result.getProperties().get("code")); // machine-readable
assertNotNull(result.getProperties().get("timestamp"));           // siempre presente

// Opcional (solo si MDC.put("traceId",...) fue llamado):
assertEquals("expected-trace-id", result.getProperties().get("traceId"));
```

## Instrucciones Críticas:
- Antes de dar por finalizada una tarea de código, preguntarme: **"¿Cumple esto con SOLID?"** y **"¿He actualizado los tests?"** y **"¿El build pasa con `mvn clean verify`?"**.
- Si la complejidad de un método crece (más de 3 niveles de anidamiento, más de 30 líneas), proponer refactorización.
- Nunca bajar la cobertura del 95%. Si un cambio implica reducirla, avisar al usuario antes de proceder.
- Al agregar un nuevo `ErrorCode`, siempre agregar su entrada en `messages.properties` Y `messages_es.properties`.

---

## 🛠️ Stack de Herramientas de Calidad

Al escribir código, debo respetar las restricciones de **todas** las herramientas activas:

| Herramienta | Estado | Lo que verifica |
|---|---|---|
| **SpotBugs** (`Effort: Max / Threshold: Low`) | ✅ Activo | Null derefs, resource leaks, race conditions |
| **JaCoCo** (≥ 95% instructions) | ✅ Activo | Cobertura de tests |
| **PMD** (`quickstart.xml`, `targetJdk: 21`) | ✅ Activo | Código muerto, complejidad ciclomática |
| **Google Java Format** (v2.25) | ✅ Activo | Formato determinístico del código |
| **Checkstyle** | ⬜ Planificado V2.0 | Convenciones de nombres, Javadoc, imports |
| **OWASP Dependency Check** | ✅ Script `scripts/dependency-check.ps1` | Vulnerabilidades en dependencias |

**Regla:** Si SpotBugs, PMD, GJF check, o JaCoCo fallan, el código NO está terminado. Siempre correr `mvn clean verify` antes de declarar una tarea lista.

---

## ✨ Buenas Prácticas Java 21 — Obligatorias al Escribir Código

### 1. Javadoc en toda API pública
Todo método y clase `public` o `protected` en `starter/` **debe** tener Javadoc:
```java
/**
 * Breve descripción en imperativo.
 *
 * @param code    descripción del parámetro
 * @param message descripción del parámetro
 * @return descripción del retorno (si no es void)
 * @throws ExceptionType cuándo se lanza (si aplica)
 */
public ProblemDetail handleProblemException(ProblemException ex, ...) { ... }
```

### 2. Final fields — Siempre que sea posible
```java
// ✅ Obligatorio en campos de dependencias inyectadas
private final MessageSource messageSource;
private final ProblemDetailService problemDetailService;
```

### 3. Records para DTOs simples (Java 16+)
Si una clase es un contenedor de datos sin lógica → usar `record`:
```java
public record ValidationError(String field, String message) {}
```

### 4. Pattern Matching en lugar de instanceof + cast (Java 21)
```java
// ✅ Usar
if (ex instanceof ErrorResponse errorResponse) {
    errorResponse.updateAndGetBody(messageSource, locale);
}
// ❌ No usar
if (ex instanceof ErrorResponse) {
    ErrorResponse errorResponse = (ErrorResponse) ex;
}
```

### 5. No Magic Numbers/Strings
```java
// ✅ Usar constantes y enums
HttpStatus.BAD_REQUEST           // no 400
ErrorCode.VALIDATION_ERROR       // no "VALIDATION_ERROR"
"error.validation.body"          // clave i18n definida en messages.properties

// ❌ Nunca
createProblem(400, req, "Invalid", "VAL", ex, locale);
```

### 6. Fail Fast con Objects.requireNonNull()
```java
// ✅ En todos los constructores con dependencias inyectadas
this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
this.enrichers     = List.copyOf(enrichers); // copia defensiva e inmutable
```

### Sealed Classes (Java 17+) — Para V2.0
`ProblemException` y sus subclases son candidatas a `sealed`. **No implementar aún** — es BREAKING CHANGE → requiere MAJOR version bump.

---

## ✅ Checklist pre-commit de código Java

Antes de declarar terminada cualquier tarea con código nuevo:
- [ ] ¿Javadoc en todos los métodos/clases públicas nuevas?
- [ ] ¿Los campos de clase son `final` donde corresponde?
- [ ] ¿Se usó `Objects.requireNonNull()` en constructores con dependencias?
- [ ] ¿No hay magic numbers/strings? ¿Se usaron constantes o enums?
- [ ] ¿Se aplicó pattern matching donde hay `instanceof` + cast?
- [ ] ¿DTOs nuevos son `record` en lugar de clase tradicional?
- [ ] ¿`mvn clean verify` pasa sin errores? (SpotBugs + JaCoCo + tests)
