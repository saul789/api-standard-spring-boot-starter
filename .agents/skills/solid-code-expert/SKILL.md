---
name: SolidCodeExpert
description: "Habilidad para garantizar código de alta calidad, siguiendo principios SOLID y requerimientos de cobertura de tests"
---

# Skill: SolidCodeExpert

Esta habilidad me obliga a aplicar los principios SOLID y mantener una estrategia de testing rigurosa en cada modificación del código.

## Principios SOLID a segur:
1. **Single Responsibility (S)**: Cada clase debe tener una única razón para cambiar. Por ejemplo, el `GlobalExceptionHandler` solo debe manejar la traducción y el mapeo, no la lógica de negocio.
2. **Open/Closed (O)**: El código debe estar abierto a la extensión pero cerrado a la modificación. Priorizamos el uso de interfaces y decoradores.
3. **Liskov Substitution (L)**: Las subclases de `ProblemException` deben poder usarse indistintamente sin romper el contrato del handler.
4. **Interface Segregation (I)**: Preferimos interfaces pequeñas y específicas. No obligamos al cliente a implementar métodos que no usa.
5. **Dependency Inversion (D)**: Dependemos de abstracciones, no de implementaciones concretas. Inyectamos `MessageSource` en lugar de instanciarlo internamente.

## Requerimientos de Cobertura (Coverage):
Siempre que se añada una funcionalidad o se refactorice código:
1. **Unit Testing Obligatorio**: Cada clase nueva debe tener su correspondiente test unitario en la carpeta `src/test/java`.
2. **Meta de Cobertura**: Intentamos mantener una cobertura mínima del **80%** en todas las clases del `starter`.
3. **Escenarios Edge-Case**: Los tests deben cubrir no solo el camino feliz, sino también errores inesperados e internacionalización (idiomas mezclados, claves inexistentes).
4. **Pruebas de Regresión**: Al modificar el Handler, debemos correr los tests existentes para asegurar que no hemos roto el estándar RFC 9457 previo.

## Instrucciones Críticas:
- Antes de dar por finalizada una tarea de código, debo preguntarme: "¿Cumple esto con SOLID?" y "¿He actualizado los tests?".
- Si la complejidad de un método crece demasiado, debo proponer una refactorización basada en estos principios.
