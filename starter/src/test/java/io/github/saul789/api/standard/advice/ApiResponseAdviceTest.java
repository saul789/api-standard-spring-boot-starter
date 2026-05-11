package io.github.saul789.api.standard.advice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.saul789.api.standard.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
class ApiResponseAdviceTest {

  @Mock private HttpServletRequest httpServletRequest;

  @InjectMocks private ApiResponseAdvice apiResponseAdvice;

  @Mock private MethodParameter returnType;

  private MediaType contentType;
  private ServerHttpRequest request;
  private ServerHttpResponse response;

  @BeforeEach
  void setUp() {
    contentType = MediaType.APPLICATION_JSON;
    request = mock(ServerHttpRequest.class);
    response = mock(ServerHttpResponse.class);
  }

  @Test
  @DisplayName("Debe retornar FALSE si el tipo de parámetro es nulo")
  void supports_ShouldReturnFalse_WhenParamTypeIsNull() {
    when(returnType.getParameterType()).thenReturn(null);

    boolean result = apiResponseAdvice.supports(returnType, null);

    assertFalse(result, "Debería ser false si el tipo es null");
  }

  @Test
  @DisplayName("Debe retornar TRUE para tipos de datos estándar (DTOs)")
  void supports_ShouldReturnTrue_ForStandardTypes() {
    when(returnType.getParameterType()).thenAnswer(invocation -> Object.class);

    boolean result = apiResponseAdvice.supports(returnType, null);

    assertTrue(result, "Debería ser true para tipos que no están excluidos");
  }

  @Test
  void shouldWrapObjectResponseIntoApiResponse() {
    Object body = new Object();
    String uri = "/api/test";
    when(httpServletRequest.getRequestURI()).thenReturn(uri);

    Object result =
        apiResponseAdvice.beforeBodyWrite(body, returnType, contentType, null, request, response);

    assertNotNull(result);
    assertTrue(result instanceof ApiResponse);
    ApiResponse<?> apiResponse = (ApiResponse<?>) result;
    assertEquals(body, apiResponse.getData());
    assertEquals("OK", apiResponse.getMessage());
    assertEquals(uri, apiResponse.getPath());
  }

  @Test
  @DisplayName("supports() debe retornar FALSE para String")
  void supports_ShouldReturnFalse_ForString() {
    when(returnType.getParameterType()).thenAnswer(i -> String.class);
    assertFalse(apiResponseAdvice.supports(returnType, null));
  }

  @Test
  @DisplayName("supports() debe retornar FALSE para byte[]")
  void supports_ShouldReturnFalse_ForByteArray() {
    when(returnType.getParameterType()).thenAnswer(i -> byte[].class);
    assertFalse(apiResponseAdvice.supports(returnType, null));
  }

  @Test
  void shouldReturnBodyAsIsWhenItIsProblemDetail() {
    ProblemDetail body = ProblemDetail.forStatus(400);
    Object result =
        apiResponseAdvice.beforeBodyWrite(body, returnType, contentType, null, request, response);
    assertSame(body, result);
    assertTrue(result instanceof ProblemDetail);
  }

  @Test
  void shouldReturnBodyAsIsWhenItIsAlreadyApiResponse() {
    ApiResponse<String> body = ApiResponse.success("already wrapped", "CUSTOM", "/path");
    Object result =
        apiResponseAdvice.beforeBodyWrite(body, returnType, contentType, null, request, response);

    assertSame(body, result);
  }

  @Test
  @DisplayName("supports() debe retornar FALSE para Resource")
  void supports_ShouldReturnFalse_ForResource() {
    when(returnType.getParameterType()).thenAnswer(i -> org.springframework.core.io.Resource.class);
    assertFalse(apiResponseAdvice.supports(returnType, null));
  }

  @Test
  @DisplayName("supports() debe retornar FALSE para subtipo de Resource")
  void supports_ShouldReturnFalse_ForResourceSubtype() {
    when(returnType.getParameterType())
        .thenAnswer(i -> org.springframework.core.io.ByteArrayResource.class);
    assertFalse(apiResponseAdvice.supports(returnType, null));
  }
}
