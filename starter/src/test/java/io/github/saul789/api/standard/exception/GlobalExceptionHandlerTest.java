package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardAutoConfiguration;
import io.github.saul789.api.standard.model.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApiStandardAutoConfiguration.class)
@Import({ GlobalExceptionHandler.class })
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();

        this.request = mock(jakarta.servlet.http.HttpServletRequest.class);
        MDC.clear();
    }

    @RestController
    static class TestController {
        @GetMapping("/business")
        public void business() {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "error.business.default", HttpStatus.BAD_REQUEST);
        }

        @GetMapping("/generic")
        public void generic() {
            throw new RuntimeException("Unexpected error");
        }

        @GetMapping("/response-status")
        public void responseStatus() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody DummyDto dto) {
            // TODO document why this method is empty
        }

        @GetMapping("/constraint")
        public void constraint() {
            // Creamos un Mock de la violación
            jakarta.validation.ConstraintViolation<?> violation = org.mockito.Mockito
                    .mock(jakarta.validation.ConstraintViolation.class);

            // Simulamos que tiene un path y un mensaje (lo que usa tu lambda)
            org.mockito.Mockito.when(violation.getPropertyPath())
                    .thenReturn(org.hibernate.validator.internal.engine.path.PathImpl.createPathFromString("email"));
            org.mockito.Mockito.when(violation.getMessage())
                    .thenReturn("formato inválido");

            // IMPORTANTE: El Set debe tener el elemento, si es Set.of() vacío o HashSet()
            // vacío, el coverage será 0%
            java.util.Set<jakarta.validation.ConstraintViolation<?>> violations = java.util.Set.of(violation);

            throw new jakarta.validation.ConstraintViolationException(violations);
        }

        @GetMapping("/weird-error")
        public void weirdError() {
            // Forzamos un status 600 (fuera de RFC)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "error", HttpStatus.valueOf(600));
        }

        @GetMapping("/business-unknown-key")
        public void businessUnknown() {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "error.non.existent.key.123",
                    HttpStatus.BAD_REQUEST);
        }
    }

    static class DummyDto {
        @NotBlank(message = "Field cannot be blank")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    void shouldEnterClientErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(404);
        when(ex.getMessage()).thenReturn("Not Found");
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldFallbackToBadRequestWhenStatusUnknown() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(499); // 🔥 clave
        when(ex.getMessage()).thenReturn("Custom error");
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldEnterServerErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(500);
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldHandleStatusBelow400() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(200); // 🔥 clave
        when(request.getRequestURI()).thenReturn("/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(ex, request);

        // Va al ELSE
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldFallbackToBadRequestWhenHttpStatusIsUnknown() {
        feign.FeignException feignEx = mock(feign.FeignException.class);

        // 499 no existe en HttpStatus
        when(feignEx.status()).thenReturn(499);
        when(feignEx.getMessage()).thenReturn("Custom client error");
        when(request.getRequestURI()).thenReturn("/api/test");

        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(feignEx, request);

        // Aquí entra al fallback
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("External client error: Custom client error",
                response.getBody().getMessage());
    }

    @Test
    void shouldHandleWeirdHttpStatus() {
        org.mockito.Mockito.when(request.getRequestURI()).thenReturn("/weird");
        globalExceptionHandler.handleGenericException(new RuntimeException("test"), request);
    }

    @Test
    void shouldReturnKeyAsDetailWhenTranslationMissing() throws Exception {
        // Definimos una llave que sabemos que NO está en messages.properties ni
        // messages_es.properties
        String unknownKey = "error.non.existent.key.123";

        mockMvc.perform(get("/business-unknown-key")
                .header("Accept-Language", "fr")) // Forzamos un idioma cualquiera
                .andExpect(status().isBadRequest())
                // Verificamos que el "detail" del JSON sea exactamente la llave
                .andExpect(jsonPath("$.detail").value(unknownKey))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldNotIncludeTraceIdWhenAbsent() throws Exception {
        MDC.clear(); // Nos aseguramos de que esté vacío
        mockMvc.perform(get("/generic"))
                .andExpect(jsonPath("$.traceId").doesNotExist());
    }

    @Test
    void shouldHandleBusinessException() throws Exception {
        mockMvc.perform(get("/business").header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void shouldHandleResponseStatusException() throws Exception {
        mockMvc.perform(get("/response-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Resource not found"));
    }

    @Test
    void shouldHandleMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void shouldHandleInvalidHttpStatusToCoverElseBranch() {
        // 1. Creamos un ProblemDetail con un status fuera del rango 100-599 (ej. 600)
        // Esto hará que (status >= 100 && status <= 599) sea FALSE
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatus(600);

        org.mockito.Mockito.when(request.getRequestURI()).thenReturn("/test-weird-status");

        // 3. Usamos Reflection para invocar el método privado 'enrich'
        // Argumentos: (instancia, nombreMetodo, arg1, arg2, arg3, arg4)
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                globalExceptionHandler,
                "enrich",
                problem,
                request,
                "error.key",
                "TEST_CODE");

        // 4. Verificamos que el título se estableció como "Error" (la rama else)
        // Esto confirma que el flujo pasó por el ': "Error"'
        org.junit.jupiter.api.Assertions.assertEquals("Error", problem.getTitle());
    }

    @Test
    void shouldCoverFalseBranchOfStatusCondition() {
        // 1. Creamos un ProblemDetail con un status que haga la condición FALSE (ej.
        // 600)
        org.springframework.http.ProblemDetail problem = org.springframework.http.ProblemDetail.forStatus(600);

        org.mockito.Mockito.when(request.getRequestURI()).thenReturn("/test-coverage");

        // 3. Invocamos el método privado 'enrich' mediante Reflection
        // Esto obligará a ejecutar la parte después de los dos puntos (: "Error")
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                globalExceptionHandler,
                "enrich",
                problem,
                request,
                "dummy.key",
                "DUMMY_CODE");

        // 4. Verificamos que el título sea "Error", confirmando que entró en la rama
        // 'else'
        org.junit.jupiter.api.Assertions.assertEquals("Error", problem.getTitle());
    }

    @Test
    void shouldCoverAllBranchesOfStatusCondition() {
        org.mockito.Mockito.when(request.getRequestURI()).thenReturn("/coverage");

        // RAMA 1: Status menor a 100 (Falla la primera condición)
        org.springframework.http.ProblemDetail problem99 = org.springframework.http.ProblemDetail.forStatus(99);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                globalExceptionHandler, "enrich", problem99, request, "key", "CODE");
        org.junit.jupiter.api.Assertions.assertEquals("Error", problem99.getTitle());

        // RAMA 2: Status mayor a 599 (Pasa la primera, falla la segunda)
        org.springframework.http.ProblemDetail problem600 = org.springframework.http.ProblemDetail.forStatus(600);
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                globalExceptionHandler, "enrich", problem600, request, "key", "CODE");
        org.junit.jupiter.api.Assertions.assertEquals("Error", problem600.getTitle());

        // NOTA: La rama donde el status está entre 100 y 599 ya está cubierta
        // por tus otros tests (BusinessException, etc.).
    }

    @Test
    void shouldHandleConstraintViolation() throws Exception {
        mockMvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldIncludeTraceIdWhenPresentInMDC() throws Exception {
        MDC.put("traceId", "test-trace-123");
        mockMvc.perform(get("/generic"))
                .andExpect(jsonPath("$.traceId").value("test-trace-123"));
    }

    @GetMapping("/constraint")
    public void constraint() {
        jakarta.validation.ConstraintViolation<?> violation = org.mockito.Mockito
                .mock(jakarta.validation.ConstraintViolation.class);

        org.mockito.Mockito.when(violation.getPropertyPath())
                .thenReturn(org.hibernate.validator.internal.engine.path.PathImpl.createPathFromString("email"));
        org.mockito.Mockito.when(violation.getMessage()).thenReturn("formato inválido");

        java.util.Set<jakarta.validation.ConstraintViolation<?>> violations = java.util.Set.of(violation);
        throw new ConstraintViolationException(violations);
    }

    @Test
    void shouldHandleConstraintViolationWithDetails() throws Exception {
        mockMvc.perform(get("/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("formato inválido"));
    }

    @Test
    void shouldMap404To404WhenFeignFails() {
        // Preparación: Simulamos un 404 de un microservicio externo
        feign.FeignException feignEx = mock(feign.FeignException.class);
        when(feignEx.status()).thenReturn(404);
        when(feignEx.getMessage()).thenReturn("Not Found");
        when(request.getRequestURI()).thenReturn("/api/my-service");

        // Ejecución
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(feignEx, request);

        // Verificación
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("External client error: Not Found", response.getBody().getMessage());
    }

    @Test
    void shouldMap500To502WhenFeignFails() {
        // Preparación: Simulamos un 500 de un microservicio externo
        feign.FeignException feignEx = mock(feign.FeignException.class);
        when(feignEx.status()).thenReturn(500);
        when(request.getRequestURI()).thenReturn("/api/my-service");

        // Ejecución
        ResponseEntity<ApiResponse<Object>> response = globalExceptionHandler.handleFeignException(feignEx, request);

        // Verificación
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("External service failure (Upstream error)", response.getBody().getMessage());
    }
}