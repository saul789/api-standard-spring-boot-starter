package io.github.saul789.api.standard.exception;
import io.github.saul789.api.standard.ApiStandardProperties;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeignExceptionHandlerTest {

    private FeignExceptionHandler feignExceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        ApiStandardProperties properties = new ApiStandardProperties();
        feignExceptionHandler = new FeignExceptionHandler(properties);
        request = mock(HttpServletRequest.class);
    }

    @Test
    void shouldEnterClientErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(404);
        when(ex.getMessage()).thenReturn("Not Found");
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals("External client error: Not Found", response.getDetail());
    }

    @Test
    void shouldFallbackToBadRequestWhenStatusUnknown() {
        feign.FeignException ex = mock(feign.FeignException.class);

        // 499 is not a standard HttpStatus that Spring Boot might resolve easily in all
        // versions
        when(ex.status()).thenReturn(499);
        when(ex.getMessage()).thenReturn("Custom error");
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void shouldEnterServerErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(500);
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
    }

    @Test
    void shouldHandleStatusBelow400() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(200);
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request);

        // Va al ELSE
        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
    }
}
