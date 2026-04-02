package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import org.springframework.context.MessageSource;

import io.github.saul789.api.standard.ApiStandardAutoConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = ApiStandardAutoConfiguration.class)
@Import({ GlobalExceptionHandler.class })
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    private GlobalExceptionHandler globalExceptionHandler;
    private MessageSource messageSource;
    private ProblemDetailService problemDetailService;

    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        this.messageSource = mock(MessageSource.class);
        // Default behavior: return the defaultMessage (arg 2)
        when(messageSource.getMessage(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        ApiStandardProperties properties = new ApiStandardProperties();
        java.util.List<ProblemDetailEnricher> enrichers = java.util.List.of(
                new StandardMetadataEnricher(),
                new TraceIdEnricher());
        this.problemDetailService = new ProblemDetailService(messageSource, properties, enrichers);
        this.globalExceptionHandler = new GlobalExceptionHandler(this.messageSource, this.problemDetailService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
        this.request = mock(HttpServletRequest.class);
    }

    @AfterEach
    void tearDown() {
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

        @GetMapping("/annotated")
        public void annotated() {
            throw new CustomAnnotatedException();
        }

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody DummyDto dto) {
            // Se usa para disparar MethodArgumentNotValidException en los tests
        }

        @GetMapping("/constraint")
        public void constraint() {
            Set<ConstraintViolation<?>> violations = new java.util.HashSet<>();

            ConstraintViolation<?> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
            org.mockito.Mockito.when(violation.getPropertyPath())
                    .thenReturn(org.hibernate.validator.internal.engine.path.PathImpl.createPathFromString("email"));
            org.mockito.Mockito.when(violation.getMessage()).thenReturn("formato inválido");
            violations.add(violation);

            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/business-unknown-key")
        public void businessUnknown() {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "error.non.existent.key.123", HttpStatus.BAD_REQUEST);
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
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/generic"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void shouldIncludeTraceIdWhenPresentInMDC() throws Exception {
        String traceIdValue = "test-uuid-12345";
        MDC.put("traceId", traceIdValue);

        try {
            mockMvc.perform(get("/generic"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.traceId").value(traceIdValue))
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void shouldReturnKeyAsDetailWhenTranslationMissing() throws Exception {
        mockMvc.perform(get("/business-unknown-key").header("Accept-Language", "fr"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("error.non.existent.key.123"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
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
    void shouldHandleWeirdHttpStatus() {
        when(request.getRequestURI()).thenReturn("/weird");
        ProblemDetail detail = globalExceptionHandler.handleGenericException(new RuntimeException("test"), request,
                java.util.Locale.ENGLISH);
        org.junit.jupiter.api.Assertions.assertNotNull(detail);
    }

    @Test
    void shouldCreateProblemThroughService() {
        ProblemDetail detail = problemDetailService.createProblem(
                HttpStatus.BAD_REQUEST, request, "error.key", "TEST_CODE", null, java.util.Locale.ENGLISH);

        org.junit.jupiter.api.Assertions.assertEquals("Bad Request", detail.getTitle());
    }

    @Test
    void shouldDirectlyHandleNoResourceFound() {
        var ex = new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET,
                "/path",
                null);

        when(request.getRequestURI()).thenReturn("/path");

        when(messageSource.getMessage(org.mockito.ArgumentMatchers.eq("error.not_found"),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("error.not_found"),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn("El recurso solicitado no fue encontrado.");

        ProblemDetail detail = globalExceptionHandler.handleNoResourceFoundException(ex, request,
                java.util.Locale.ENGLISH);

        org.junit.jupiter.api.Assertions.assertEquals(404, detail.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("El recurso solicitado no fue encontrado.", detail.getDetail());
    }

    @Test
    void shouldHandleServiceEnrichWithValidStatusButNoTitleTranslation() {
        when(request.getRequestURI()).thenReturn("/payment");

        ProblemDetail problem = problemDetailService.createProblem(
                HttpStatus.PAYMENT_REQUIRED, request, "error.test", "UNKNOWN_CODE", null, java.util.Locale.ENGLISH);

        // Debería tomar el Reason Phrase de HttpStatus ("Payment Required")
        org.junit.jupiter.api.Assertions.assertEquals("Payment Required", problem.getTitle());
    }

    @Test
    void shouldHandleValidationBodyError() throws Exception {
        mockMvc.perform(post("/validation")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"name\": \"\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    void shouldHandleMethodNotAllowed() throws Exception {
        // Intentamos un POST en un endpoint que solo permite GET
        mockMvc.perform(post("/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void shouldHandleHttpMediaTypeNotSupported() throws Exception {
        // Enviamos un Content-Type que el controller no espera
        mockMvc.perform(post("/validation")
                .contentType("application/xml")
                .content("<xml></xml>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldHandleResponseStatusException() throws Exception {
        mockMvc.perform(get("/response-status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldHandleAccessDeniedException() throws Exception {
        // Simulamos la excepción de Spring Security por nombre
        mockMvc = MockMvcBuilders.standaloneSetup(new SecurityTestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();

        mockMvc.perform(get("/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("error.forbidden"));
    }

    @RestController
    static class SecurityTestController {
        @GetMapping("/access-denied")
        public void accessDenied() {
            throw new org.springframework.security.access.AccessDeniedException("Denied");
        }
    }

    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CONFLICT)
    static class CustomAnnotatedException extends RuntimeException {
    }

    @Test
    void shouldHandleAnnotatedException() throws Exception {
        mockMvc.perform(get("/annotated"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

}