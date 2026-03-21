package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardAutoConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.test.util.ReflectionTestUtils;
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

@SpringBootTest(classes = ApiStandardAutoConfiguration.class)
@Import({ GlobalExceptionHandler.class })
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private jakarta.servlet.http.HttpServletRequest request;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
        this.request = mock(jakarta.servlet.http.HttpServletRequest.class);
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

        @PostMapping("/validation")
        public void validation(@Valid @RequestBody DummyDto dto) {
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
        ProblemDetail detail = globalExceptionHandler.handleGenericException(new RuntimeException("test"), request);
        org.junit.jupiter.api.Assertions.assertNotNull(detail);
    }

    @Test
    void shouldHandleInvalidHttpStatusToCoverElseBranch() {
        ProblemDetail problem = ProblemDetail.forStatus(600);
        when(request.getRequestURI()).thenReturn("/test-weird-status");

        ReflectionTestUtils.invokeMethod(globalExceptionHandler, "enrich", problem, request, "error.key", "TEST_CODE");

        org.junit.jupiter.api.Assertions.assertEquals("Error", problem.getTitle());
    }
}