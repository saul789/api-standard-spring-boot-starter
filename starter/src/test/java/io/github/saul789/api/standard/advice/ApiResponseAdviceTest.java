package io.github.saul789.api.standard.advice;

import io.github.saul789.api.standard.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResponseAdviceTest {

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private ApiResponseAdvice apiResponseAdvice;

    // Mocks auxiliares para los parámetros del método beforeBodyWrite
    private MethodParameter returnType;
    private MediaType contentType;
    private ServerHttpRequest request;
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() {
        returnType = mock(MethodParameter.class);
        contentType = MediaType.APPLICATION_JSON;
        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
    }

    @Test
    void supportsShouldAlwaysReturnTrue() {
        // Cubre el método supports al 100%
        assertTrue(apiResponseAdvice.supports(returnType, null));
    }

    @Test
    void shouldWrapSimpleResponseIntoApiResponse() {
        // Preparación
        String body = "Hello World";
        String uri = "/api/test";
        when(httpServletRequest.getRequestURI()).thenReturn(uri);

        // Ejecución
        Object result = apiResponseAdvice.beforeBodyWrite(
                body, returnType, contentType, null, request, response);

        // Verificación (Cubre la rama por defecto / éxito)
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result;
        assertEquals(body, apiResponse.getData());
        assertEquals("OK", apiResponse.getMessage());
        assertEquals(uri, apiResponse.getPath());
    }

    @Test
    void shouldReturnBodyAsIsWhenItIsProblemDetail() {
        // Preparación
        ProblemDetail body = ProblemDetail.forStatus(400);

        // Ejecución
        Object result = apiResponseAdvice.beforeBodyWrite(
                body, returnType, contentType, null, request, response);

        // Verificación (Cubre la rama: if (body instanceof ProblemDetail))
        assertSame(body, result);
        assertTrue(result instanceof ProblemDetail);
    }

    @Test
    void shouldReturnBodyAsIsWhenItIsAlreadyApiResponse() {
        // Preparación
        ApiResponse<String> body = ApiResponse.success("already wrapped", "CUSTOM", "/path");

        // Ejecución
        Object result = apiResponseAdvice.beforeBodyWrite(
                body, returnType, contentType, null, request, response);

        // Verificación (Cubre la rama: if (body instanceof ApiResponse<?>))
        assertSame(body, result);
    }
}