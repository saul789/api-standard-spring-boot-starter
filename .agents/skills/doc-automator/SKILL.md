---
name: DocAutomator
description: "Sincronizador automático de documentación técnica para el API standard starter"
capabilities: [ "README.md", "Postman", "Spring Boot" ]
---

# Skill: DocAutomator

Esta habilidad está diseñada para garantizar que la documentación sea siempre coherente con el código base del starter de estandarización de APIs.

## Funcionamiento:
Siempre que realices un cambio en la lógica de manejo de excepciones o añadas un nuevo endpoint de demostración en el `sample-project`, **automáticamente** debes realizar estas tres acciones:

1. **Actualización de README.md**:
   - Reflejar los nuevos ejemplos JSON de error en las secciones correspondientes.
   - Añadir explicaciones sobre nuevos parámetros o funcionalidades de personalización.
   - Mantener las versiones de Java/Spring Boot actualizadas.

2. **Actualización de la Colección de Postman**:
   - Añadir el nuevo request a la carpeta correspondiente.
   - Incluir scripts de prueba (`pm.test`) que validen campos RFC 9457 (type, title, status, code, traceId, timestamp).
   - Asegurar que las variables globales de Postman (`base_url`) sigan funcionando.

3. **Mantenimiento de i18n**:
   - Verificar si el nuevo endpoint o excepción requiere una entrada en `messages.properties` y `messages_es.properties`.
   - Si no existe, créala siguiendo la nomenclatura `error.<CODE_NAME>`.

## Instrucciones de Uso:
- Este skill debe activarse al detectar cambios en `GlobalExceptionHandler.java`, `FeignExceptionHandler.java` o en los controladores de ejemplo del `sample-project`.
- No debe dejarse la documentación desactualizada por más de un turno de conversación.
- Las respuestas JSON en el README deben ser sintácticamente correctas y contener el campo `type` con el formato URN/URL adecuado.
