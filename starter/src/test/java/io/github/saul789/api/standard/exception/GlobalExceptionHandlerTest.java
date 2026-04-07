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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

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
    void shouldHandleGenericExceptionDirectly() {
        var ex = new RuntimeException("Direct error");
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertEquals("error.internal_error", result.getDetail());
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
    void shouldHandleValidationExceptionWithI18n() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("username");
        when(fieldError.getDefaultMessage()).thenReturn("default message");
        when(fieldError.getCodes()).thenReturn(new String[] { "abc", "def" });
        when(fieldError.getArguments()).thenReturn(new Object[] {});

        // Mock messageSource to return a translated message for code "abc"
        when(messageSource.getMessage(eq("abc"), any(), any(), any(Locale.class)))
                .thenReturn("translated message");

        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail detail = globalExceptionHandler.handleValidationException(ex, request, Locale.ENGLISH);

        assertNotNull(detail);
        assertEquals(400, detail.getStatus());

        @SuppressWarnings("unchecked")
        List<ValidationError> errors = (List<ValidationError>) detail.getProperties().get("errors");
        assertNotNull(errors);
        assertEquals(1, errors.size());
        assertEquals("username", errors.get(0).getField());
        assertEquals("translated message", errors.get(0).getMessage());
    }

    @Test
    void shouldHandleValidationExceptionWithDefaultMessageI18n() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("username");
        when(fieldError.getDefaultMessage()).thenReturn("error.key");
        when(fieldError.getCodes()).thenReturn(new String[] { "invalid.code" });
        when(fieldError.getArguments()).thenReturn(new Object[] {});

        // Return null for the specific error code to trigger fallback to default
        // message
        when(messageSource.getMessage(eq("invalid.code"), any(), any(), any(Locale.class)))
                .thenReturn(null);

        // Return translation for the default message key
        when(messageSource.getMessage(eq("error.key"), any(), eq("error.key"), any(Locale.class)))
                .thenReturn("translated default message");

        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail detail = globalExceptionHandler.handleValidationException(ex, request, Locale.ENGLISH);

        assertNotNull(detail);
        @SuppressWarnings("unchecked")
        List<ValidationError> errors = (List<ValidationError>) detail.getProperties().get("errors");
        assertEquals("translated default message", errors.get(0).getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleValidationExceptionWithNoCodes() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("username");
        when(fieldError.getDefaultMessage()).thenReturn("default");
        when(fieldError.getCodes()).thenReturn(new String[] {}); // Empty codes

        ProblemDetail detail = globalExceptionHandler.handleValidationException(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) detail.getProperties().get("errors");
        assertEquals("default", errors.get(0).getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleValidationExceptionWithMatchingCodeAndMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("username");
        when(fieldError.getDefaultMessage()).thenReturn("default");
        when(fieldError.getCodes()).thenReturn(new String[] { "code1" });
        // messageSource returns same as code
        when(messageSource.getMessage(eq("code1"), any(), any(), any())).thenReturn("code1");

        ProblemDetail detail = globalExceptionHandler.handleValidationException(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) detail.getProperties().get("errors");
        assertEquals("default", errors.get(0).getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleConstraintViolationWithMalformedTemplate() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.iterator()).thenReturn(java.util.Collections.emptyIterator());
        when(violation.getMessage()).thenReturn("raw message");
        when(violation.getMessageTemplate()).thenReturn("no-braces"); // Malformed for i18n

        ProblemDetail result = globalExceptionHandler.handleConstraintViolation(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) result.getProperties().get("errors");
        assertEquals("raw message", errors.get(0).getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleConstraintViolationWithPartialTemplate() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.iterator()).thenReturn(java.util.Collections.emptyIterator());
        when(violation.getMessage()).thenReturn("raw message");

        when(violation.getMessageTemplate()).thenReturn("{missing-end");
        ProblemDetail result1 = globalExceptionHandler.handleConstraintViolation(ex, request, Locale.ENGLISH);
        assertEquals("raw message",
                ((List<ValidationError>) result1.getProperties().get("errors")).get(0).getMessage());

        when(violation.getMessageTemplate()).thenReturn("missing-start}");
        ProblemDetail result2 = globalExceptionHandler.handleConstraintViolation(ex, request, Locale.ENGLISH);
        assertEquals("raw message",
                ((List<ValidationError>) result2.getProperties().get("errors")).get(0).getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleValidationExceptionWithNullMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = mock(FieldError.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(fieldError.getField()).thenReturn("username");
        when(fieldError.getDefaultMessage()).thenReturn(null); // NULL default message
        when(fieldError.getCodes()).thenReturn(new String[] {});

        ProblemDetail detail = globalExceptionHandler.handleValidationException(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) detail.getProperties().get("errors");
        assertNull(errors.get(0).getMessage());
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
    @SuppressWarnings("unchecked")
    void shouldHandleConstraintViolationWithFieldNames() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        Path.Node node = mock(Path.Node.class);

        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));
        when(violation.getPropertyPath()).thenReturn(path);
        when(path.iterator()).thenReturn(List.of(node).iterator());
        when(node.getName()).thenReturn("username");
        when(violation.getMessage()).thenReturn("invalid");

        ProblemDetail result = globalExceptionHandler.handleConstraintViolation(ex, request, Locale.ENGLISH);
        List<ValidationError> errors = (List<ValidationError>) result.getProperties().get("errors");
        assertEquals("username", errors.get(0).getField());
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
    void shouldHandleAccessDeniedExceptionByName() {
        // Mock exception where simple name contains AccessDeniedException
        class FakeAccessDeniedException extends RuntimeException {
        }
        var ex = new FakeAccessDeniedException();

        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
        assertEquals("FORBIDDEN", result.getProperties().get("code"));
    }

    @Test
    void shouldHandleResponseStatusException() {
        var ex = new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_ACCEPTABLE,
                "Not Acceptable");
        ProblemDetail result = globalExceptionHandler.handleResponseStatusException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), result.getStatus());
    }

    @Test
    void shouldHandleNoResourceFoundException() {
        var ex = mock(org.springframework.web.servlet.resource.NoResourceFoundException.class);
        ProblemDetail result = globalExceptionHandler.handleNoResourceFoundException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
    }

    @Test
    void shouldHandleMediaTypeNotSupportedException() {
        var ex = mock(org.springframework.web.HttpMediaTypeNotSupportedException.class);
        ProblemDetail result = globalExceptionHandler.handleMediaTypeNotSupported(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), result.getStatus());
    }

    @Test
    void shouldHandleMethodNotAllowedException() {
        var ex = mock(org.springframework.web.HttpRequestMethodNotSupportedException.class);
        ProblemDetail result = globalExceptionHandler.handleMethodNotAllowed(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), result.getStatus());
    }

    @Test
    void shouldHandleProblemException() {
        var ex = new ProblemException(ErrorCode.BAD_REQUEST, "Custom message", HttpStatus.BAD_REQUEST);
        ProblemDetail result = globalExceptionHandler.handleProblemException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Custom message", result.getDetail());
    }

    @Test
    void shouldCoverAllProblemExceptionConstructors() {
        var uri = java.net.URI.create("http://test.com");

        // ProblemException variations
        assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST));
        assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
        assertNotNull(new ProblemException("http://custom.com", "msg", HttpStatus.BAD_REQUEST));
        assertNotNull(new ProblemException("msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
        assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (java.net.URI) null));
        assertNotNull(new ProblemException("msg", HttpStatus.BAD_GATEWAY));

        // BusinessException variations
        assertNotNull(new BusinessException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST));
        assertNotNull(new BusinessException("msg", HttpStatus.BAD_REQUEST));
        assertNotNull(new BusinessException("msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
        assertNotNull(new BusinessException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_GATEWAY, "detail"));
        assertNotNull(new BusinessException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, uri));

        // Edge case for branch coverage: String customUrl is null
        assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (String) null));
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
            @Override
            public java.net.URI getProblemType() {
                return java.net.URI.create("http://provider.com");
            }
        }
        ProblemDetail result = globalExceptionHandler.handleGenericException(new ProviderException(), request,
                Locale.ENGLISH);
        assertEquals("http://provider.com", result.getType().toString());
    }

    @Test
    void shouldHandleAnnotatedProblemTypeException() {
        @ProblemType("http://annotated.com")
        class AnnotatedEx extends RuntimeException {
        }
        ProblemDetail result = globalExceptionHandler.handleGenericException(new AnnotatedEx(), request,
                Locale.ENGLISH);
        assertEquals("http://annotated.com", result.getType().toString());
    }

    @Test
    void shouldHandleProblemDetailServiceReflection() throws Exception {
        java.lang.reflect.Method resolveType = ProblemDetailService.class.getDeclaredMethod("resolveType", String.class,
                java.net.URI.class);
        resolveType.setAccessible(true);

        // Case: null code
        Object resultNull = resolveType.invoke(problemDetailService, null, null);
        assertEquals("urn:problem-type:unknown-error", resultNull.toString());

        // Case: blank code
        Object resultBlank = resolveType.invoke(problemDetailService, "  ", null);
        assertEquals("urn:problem-type:unknown-error", resultBlank.toString());

        // Case 2: Ends with a closing brace but does not start with an opening bracekebab)
        Object resultInvalid = resolveType.invoke(problemDetailService, "CUSTOM_ERROR", null);
        assertEquals("urn:problem-type:custom-error", resultInvalid.toString());
    }

    @Test
    void shouldHandleExtractCustomTypeEdgeCases() throws Exception {
        java.lang.reflect.Method extractType = ProblemDetailService.class.getDeclaredMethod("extractCustomType",
                Exception.class);
        extractType.setAccessible(true);

        // Case: null exception
        assertNull(extractType.invoke(problemDetailService, (Object) null));

        // Case 1: Starts with an opening brace but does not end with a closing braceout annotation
        assertNull(extractType.invoke(problemDetailService, new RuntimeException()));

        // Case: annotated with invalid URI
        @ProblemType("invalid uri with spaces")
        class InvalidAnnotatedEx extends RuntimeException {
        }
        assertNull(extractType.invoke(problemDetailService, new InvalidAnnotatedEx()));
    }

    @Test
    void shouldHandleInvalidTypeOverrideGracefully() {
        ApiStandardProperties props = new ApiStandardProperties();
        props.getErrors().setTypeOverrides(java.util.Map.of("BAD_REQUEST", "invalid uri with spaces"));
        ProblemDetailService pds = new ProblemDetailService(messageSource, props, List.of());
        ProblemDetail result = pds.createProblem(HttpStatus.BAD_REQUEST, request, "detail", "BAD_REQUEST", null,
                Locale.ENGLISH);
        assertEquals("urn:problem-type:bad-request", result.getType().toString());
    }

    @Test
    void shouldHandleStatusReasonPhraseFailure() {
        HttpStatus status = mock(HttpStatus.class);
        // ProblemDetail.forStatus(status) uses status.value()
        when(status.value()).thenReturn(500);
        // status.getReasonPhrase() is used in the try-catch
        when(status.getReasonPhrase()).thenThrow(new RuntimeException("Simulation"));

        ProblemDetailService pds = new ProblemDetailService(messageSource, new ApiStandardProperties(), List.of());
        ProblemDetail result = pds.createProblem(status, request, "detail", "INTERNAL_ERROR", null, Locale.ENGLISH);
        assertEquals(500, result.getStatus());
        // messageSource.getMessage("error.INTERNAL_ERROR", null, "Error", locale)
        // Our mock returns arg 2 if translation fails, which would be "Error"
        assertEquals("Error", result.getTitle());
    }

    @Test
    void shouldHandleValidTypeOverrideInService() {
        ApiStandardProperties props = new ApiStandardProperties();
        props.getErrors().setTypeOverrides(java.util.Map.of("CUSTOM_CODE", "http://valid-override.com"));
        ProblemDetailService pds = new ProblemDetailService(messageSource, props, List.of());
        ProblemDetail result = pds.createProblem(HttpStatus.BAD_REQUEST, request, "detail", "CUSTOM_CODE", null,
                Locale.ENGLISH);
        assertEquals("http://valid-override.com", result.getType().toString());
    }

    @Test
    void shouldHandleAnnotatedException() {
        var ex = new CustomAnnotatedException();
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
    }

    @Test
    void shouldHandleTimeoutExceptionAs504() {
        var ex = new java.util.concurrent.TimeoutException("upstream timeout");
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT.value(), result.getStatus());
        assertEquals("GATEWAY_TIMEOUT", result.getProperties().get("code"));
    }

    @Test
    void shouldHandleResilience4jCircuitBreakerAs503() {
        // Mock actual Resilience4j exception so that the class name contains the
        // package name
        var ex = mock(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class);
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), result.getStatus());
        assertEquals("SERVICE_UNAVAILABLE", result.getProperties().get("code"));
    }

    @Test
    void shouldHandleResilience4jRateLimiterAs429() {
        var ex = mock(io.github.resilience4j.ratelimiter.RequestNotPermitted.class);
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), result.getStatus());
        assertEquals("TOO_MANY_REQUESTS", result.getProperties().get("code"));
    }

    @Test
    void shouldHandleResilience4jBulkheadAs429() {
        var ex = mock(io.github.resilience4j.bulkhead.BulkheadFullException.class);
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), result.getStatus());
        assertEquals("TOO_MANY_REQUESTS", result.getProperties().get("code"));
    }

    @Test
    void shouldCoverSetTypeOverridesNull() {
        ApiStandardProperties props = new ApiStandardProperties();
        props.getErrors().setTypeOverrides(null);
        assertTrue(props.getErrors().getTypeOverrides().isEmpty());
    }

    @Test
    void shouldHandleGenericExceptionWithAccessDenied() {
        class FakeAccessDeniedException extends RuntimeException {
        }
        var ex = new FakeAccessDeniedException();
        ProblemDetail result = globalExceptionHandler.handleGenericException(ex, request, Locale.ENGLISH);
        assertEquals(HttpStatus.FORBIDDEN.value(), result.getStatus());
        assertEquals("FORBIDDEN", result.getProperties().get("code"));
    }

    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CONFLICT)
    static class CustomAnnotatedException extends RuntimeException {
    }
}