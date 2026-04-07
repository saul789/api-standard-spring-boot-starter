# Sample Project Controllers

Este paquete contiene ejemplos de cómo utilizar y probar las características del `spring-boot-starter-api-standard`.

## 📁 Estructura de Ejemplos

### 1. [StandardResponseController.java](./StandardResponseController.java)
Demuestra el **éxito** automático. Cualquier controlador que devuelva un objeto (DTO, List, Map) será envuelto en un objeto `ApiResponse` estandarizado.
- **GET /api/demo/responses/object**: Retorna un `UserDto`.
- **GET /api/demo/responses/list**: Retorna una colección.
- **GET /api/demo/responses/raw**: Muestra que los `String` no se envuelven para permitir respuestas de texto plano si se desea.

### 2. [ValidationDemoController.java](./ValidationDemoController.java)
Demuestra la **validación** automática. Utiliza las anotaciones de JSR-303 (`@Valid`, `@NotBlank`, etc.).
- **POST /api/demo/validation/body**: Error de validación en el cuerpo JSON (`MethodArgumentNotValidException`).
- **GET /api/demo/validation/params**: Error de validación en parámetros de consulta (`ConstraintViolationException`).

### 3. [ExceptionDemoController.java](./ExceptionDemoController.java)
Muestra la flexibilidad del **Global Error Handling**.
- **GET /api/demo/exceptions/problem-internal**: Usa la URN estándar generada automáticamente por la librería.
- **GET /api/demo/exceptions/problem-external**: Muestra cómo proporcionar una URL propia para anular la URN estándar.
- **GET /api/demo/exceptions/business-logic**: Uso de `BusinessException`.
- **GET /api/demo/exceptions/annotated**: Soporte inteligente para `@ResponseStatus` en excepciones personalizadas.
- **GET /api/demo/exceptions/not-found**: Manejo de `ResponseStatusException`.
- **GET /api/demo/exceptions/server-error**: Manejo de errores no capturados (fallback 500).
- **GET /api/demo/exceptions/i18n-key**: Traducción automática de mensajes (soporte i18n).
- **GET /api/demo/exceptions/custom-message**: Mensaje personalizado que no es una clave.
- **GET /api/demo/exceptions/client-key**: Mensaje de error personalizado en el `messages.properties` del proyecto cliente.

### 4. [FeignDemoController.java](./FeignDemoController.java)
Demuestra la integración con **OpenFeign**.
- **GET /api/demo/feign/upstream-failure**: Convierte errores 5xx del cliente en un estándar `502 Bad Gateway`.
- **GET /api/demo/feign/upstream-notfound**: Propaga errores 4xx del cliente (como 404) para informar al consumidor final.

### 5. [ResilienceDemoController.java](./ResilienceDemoController.java)
Demuestra el manejo estandarizado de excepciones de resiliencia con **Resilience4j**.
- **GET /api/demo/resilience/circuit-breaker**: Demostración de `CallNotPermittedException` enviada como `503 Service Unavailable`.
- **GET /api/demo/resilience/rate-limiter**: Demostración de `RequestNotPermitted` enviada como `429 Too Many Requests`.
- **GET /api/demo/resilience/bulkhead**: Demostración de `BulkheadFullException` enviada como `429 Too Many Requests`.
- **GET /api/demo/resilience/timeout**: Demostración de `TimeoutException` enviada como `504 Gateway Timeout`.

### 6. [UserController.java](./UserController.java)
Controlador básico que demuestra operaciones CRUD típicas.
