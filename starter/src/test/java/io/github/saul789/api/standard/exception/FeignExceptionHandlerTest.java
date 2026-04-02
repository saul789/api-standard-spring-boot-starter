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

    private org.springframework.context.MessageSource messageSource;

    @BeforeEach
    void setUp() {
        this.messageSource = mock(org.springframework.context.MessageSource.class);
        // Default behavior: return the defaultMessage (arg 2)
        when(messageSource.getMessage(org.mockito.ArgumentMatchers.anyString(), 
                                    org.mockito.ArgumentMatchers.any(), 
                                    org.mockito.ArgumentMatchers.anyString(), 
                                    org.mockito.ArgumentMatchers.any()))
            .thenAnswer(invocation -> invocation.getArgument(2));

        ApiStandardProperties properties = new ApiStandardProperties();
        feignExceptionHandler = new FeignExceptionHandler(properties, messageSource);
        request = mock(jakarta.servlet.http.HttpServletRequest.class);
    }

    @Test
    void shouldEnterClientErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(404);
        when(ex.getMessage()).thenReturn("Not Found");
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals("Upstream service reported client-side error: Not Found", response.getDetail());
        assertEquals("Not Found", response.getTitle());
    }

    @Test
    void shouldFallbackToBadRequestWhenStatusUnknown() {
        feign.FeignException ex = mock(feign.FeignException.class);

        // 499 is not a standard HttpStatus that Spring Boot might resolve easily in all
        // versions
        when(ex.status()).thenReturn(499);
        when(ex.getMessage()).thenReturn("Custom error");
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void shouldEnterServerErrorBranch() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(500);
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
    }

    @Test
    void shouldHandleStatusBelow400() {
        feign.FeignException ex = mock(feign.FeignException.class);

        when(ex.status()).thenReturn(200);
        when(request.getRequestURI()).thenReturn("/test");

        ProblemDetail response = feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

        // Va al ELSE
        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
    }
}
