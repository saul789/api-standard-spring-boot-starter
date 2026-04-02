package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import org.springframework.context.MessageSource;
import io.github.saul789.api.standard.ApiStandardAutoConfiguration;
import io.github.saul789.api.standard.model.ValidationError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(classes = ApiStandardAutoConfiguration.class)
@Import({ GlobalExceptionHandler.class })
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private GlobalExceptionHandler globalExceptionHandler;
    private MessageSource messageSource;
    private ProblemDetailService problemDetailService;
    private HttpServletRequest request;
    private ApiStandardProperties properties;

    @BeforeEach
    void setUp() {
        this.messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));

        this.properties = new ApiStandardProperties();
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
        @GetMapping("/generic")
        public void generic() {
            throw new RuntimeException("Unexpected error");
        }

        @GetMapping("/response-status")
        public void responseStatus() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }

        @PostMapping("/validation")
        public void validation(@jakarta.validation.Valid @RequestBody DummyDto dto) {
            // Test body
        }

        @GetMapping("/constraint")
        public void constraint() {
            Set<ConstraintViolation<?>> violations = new java.util.HashSet<>();
            ConstraintViolation<?> violation = mock(ConstraintViolation.class);
            when(violation.getPropertyPath())
                    .thenReturn(org.hibernate.validator.internal.engine.path.PathImpl.createPathFromString("email"));
            when(violation.getMessage()).thenReturn("formato inválido");
            violations.add(violation);
            throw new ConstraintViolationException(violations);
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
        mockMvc.perform(MockMvcRequestBuilders.get("/generic"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void shouldIncludeTraceIdWhenPresentInMDC() throws Exception {
        String traceIdValue = "test-uuid-12345";
        MDC.put("traceId", traceIdValue);
        try {
            mockMvc.perform(MockMvcRequestBuilders.get("/generic"))
                    .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.traceId").value(traceIdValue));
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    void shouldHandleConstraintViolationWithDetails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/constraint"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldDirectlyHandleNoResourceFound() {
        var ex = new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/path", null);
        when(request.getRequestURI()).thenReturn("/path");
        ProblemDetail detail = globalExceptionHandler.handleNoResourceFoundException(ex, request, Locale.ENGLISH);
        assertEquals(404, detail.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleConstraintViolationWithI18nTemplate() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.iterator()).thenReturn(java.util.Collections.emptyIterator());
        when(violation.getMessage()).thenReturn("default");
        when(violation.getMessageTemplate()).thenReturn("{error.test}");
        when(messageSource.getMessage(eq("error.test"), any(), any(), any())).thenReturn("translated");

        ProblemDetail result = globalExceptionHandler.handleConstraintViolation(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) result.getProperties().get("errors");
        assertEquals("translated", errors.get(0).getMessage());
    }

    @Test
    void shouldHandleGenericErrorResponseException() {
        var ex = mock(org.springframework.web.ErrorResponseException.class);
        ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "conflict-detail");
        when(ex.updateAndGetBody(any(), any())).thenReturn(body);
        when(ex.getStatusCode()).thenReturn(HttpStatus.CONFLICT);
        when(ex.getMessage()).thenReturn("msg");

        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("CONFLICT", result.getProperties().get("code"));
    }

    @Test
    void shouldCoverAllProblemExceptionConstructors() {
        var uri = java.net.URI.create("http://test.com");
        new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST);
        var ex2 = new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, "http://custom.com");
        assertEquals("msg", ex2.getMessage());

        new BusinessException("msg", HttpStatus.BAD_REQUEST);
        new BusinessException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, "detail");
        new BusinessException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, uri);
    }

    @Test
    void shouldCoverRemainingErrorCodeMappings() {
        assertEquals(ErrorCode.GONE, ErrorCode.fromStatus(410));
        assertEquals(ErrorCode.VALIDATION_ERROR, ErrorCode.fromStatus(422));
        assertEquals(ErrorCode.TOO_MANY_REQUESTS, ErrorCode.fromStatus(429));
        assertEquals(ErrorCode.UNAUTHORIZED, ErrorCode.fromStatus(401));
        assertEquals(ErrorCode.FORBIDDEN, ErrorCode.fromStatus(403));
        assertEquals(ErrorCode.NOT_FOUND, ErrorCode.fromStatus(404));
        assertEquals(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.fromStatus(405));
        assertEquals(ErrorCode.CONFLICT, ErrorCode.fromStatus(409));
        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ErrorCode.fromStatus(415));
        assertEquals(ErrorCode.BAD_GATEWAY, ErrorCode.fromStatus(502));
        assertEquals(ErrorCode.SERVICE_UNAVAILABLE, ErrorCode.fromStatus(503));
        assertEquals(ErrorCode.GATEWAY_TIMEOUT, ErrorCode.fromStatus(504));
        assertEquals(ErrorCode.INTERNAL_ERROR, ErrorCode.fromStatus(500));
    }

    @Test
    void shouldHandleProblemTypeProviderException() {
        class ProviderException extends RuntimeException implements ProblemTypeProvider {
            @Override public java.net.URI getProblemType() { return java.net.URI.create("http://provider.com"); }
        }
        ProblemDetail result = globalExceptionHandler.handleGenericException(new ProviderException(), request, Locale.ENGLISH);
        assertEquals("http://provider.com", result.getType().toString());
    }

    @Test
    void shouldHandleAnnotatedProblemTypeException() {
        @ProblemType("http://annotated.com")
        class AnnotatedEx extends RuntimeException {}
        ProblemDetail result = globalExceptionHandler.handleGenericException(new AnnotatedEx(), request, Locale.ENGLISH);
        assertEquals("http://annotated.com", result.getType().toString());
    }

    @Test
    void shouldHandleProblemDetailServiceReflection() throws Exception {
        java.lang.reflect.Method resolveType = ProblemDetailService.class.getDeclaredMethod("resolveType", String.class,
                java.net.URI.class);
        resolveType.setAccessible(true);
        Object result = resolveType.invoke(problemDetailService, null, null);
        assertEquals("urn:problem-type:unknown-error", result.toString());
    }

    @Test
    void shouldHandleInvalidTypeOverrideGracefully() {
        ApiStandardProperties props = new ApiStandardProperties();
        props.getErrors().getTypeOverrides().put("BAD_REQUEST", "invalid uri with spaces");
        ProblemDetailService pds = new ProblemDetailService(messageSource, props, List.of());
        ProblemDetail result = pds.createProblem(HttpStatus.BAD_REQUEST, request, "detail", "BAD_REQUEST", null, Locale.ENGLISH);
        assertEquals("urn:problem-type:bad-request", result.getType().toString());
    }

    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CONFLICT)
    static class CustomAnnotatedException extends RuntimeException {
    }
}