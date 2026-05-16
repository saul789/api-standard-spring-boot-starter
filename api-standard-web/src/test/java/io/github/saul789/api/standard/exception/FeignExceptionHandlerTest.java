package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class FeignExceptionHandlerTest {

  private FeignExceptionHandler feignExceptionHandler;
  private HttpServletRequest request;

  private org.springframework.context.MessageSource messageSource;

  @BeforeEach
  void setUp() {
    this.messageSource = mock(org.springframework.context.MessageSource.class);
    // Default behavior: return the defaultMessage (arg 2)
    when(messageSource.getMessage(
            org.mockito.ArgumentMatchers.anyString(),
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

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

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

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
  }

  @Test
  void shouldEnterServerErrorBranch() {
    feign.FeignException ex = mock(feign.FeignException.class);

    when(ex.status()).thenReturn(500);
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

    assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
  }

  @Test
  void shouldHandleStatusBelow400() {
    feign.FeignException ex = mock(feign.FeignException.class);

    when(ex.status()).thenReturn(200);
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

    // Va al ELSE
    assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
  }

  @Test
  void shouldParseValidProblemDetailFromJson() {
    feign.FeignException ex = mock(feign.FeignException.class);
    String json =
        "{\"type\":\"http://example.com\", \"title\":\"Original Title\", \"detail\":\"Original Detail\", \"code\":\"ORIGINAL_CODE\"}";

    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(json);
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

    assertEquals("Original Detail", response.getDetail());
    assertEquals("ORIGINAL_CODE", response.getProperties().get("code"));
  }

  @Test
  void shouldHandleInvalidJsonInContent() {
    feign.FeignException ex = mock(feign.FeignException.class);
    String invalidJson = "{ invalid }";

    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(invalidJson);
    when(ex.getMessage()).thenReturn("Standard Message");
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);

    // Debe usar el mensaje de la excepción por el catch
    assertEquals(
        "Upstream service reported client-side error: Standard Message", response.getDetail());
  }

  @Test
  void shouldHandleNullContentGracefully() {
    feign.FeignException ex = mock(feign.FeignException.class);

    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(null);
    when(ex.getMessage()).thenReturn("Safe Message");
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals("Upstream service reported client-side error: Safe Message", response.getDetail());
  }

  @Test
  void shouldHandleFeignExceptionWithTypeOverride() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(400);
    when(request.getRequestURI()).thenReturn("/test");

    ApiStandardProperties props = new ApiStandardProperties();
    props.getErrors().setTypeOverrides(java.util.Map.of("BAD_REQUEST", "http://overridden.com"));
    FeignExceptionHandler customHandler = new FeignExceptionHandler(props, messageSource);

    ProblemDetail response =
        customHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals("http://overridden.com", response.getType().toString());
  }

  @Test
  void shouldIncludeTraceIdInFeignResponse() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(400);
    when(request.getRequestURI()).thenReturn("/test");

    org.slf4j.MDC.put("traceId", "feign-trace");
    try {
      ProblemDetail response =
          feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
      assertEquals("feign-trace", response.getProperties().get("traceId"));
    } finally {
      org.slf4j.MDC.remove("traceId");
    }
  }

  @Test
  void shouldHandleFeignExceptionWithInvalidTypeOverride() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(400);
    when(request.getRequestURI()).thenReturn("/test");

    ApiStandardProperties props = new ApiStandardProperties();
    // Invalid URI string with spaces
    props.getErrors().setTypeOverrides(java.util.Map.of("BAD_REQUEST", "invalid uri with spaces"));
    FeignExceptionHandler customHandler = new FeignExceptionHandler(props, messageSource);

    ProblemDetail response =
        customHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    // Should fallback to default type
    assertEquals("urn:problem-type:bad-request", response.getType().toString());
  }

  @Test
  void shouldHandleFeignExceptionWithEmptyContent() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(""); // Blank
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals(400, response.getStatus());
  }

  @Test
  void shouldHandleFeignExceptionWithNullRequestUri() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(400);
    when(request.getRequestURI()).thenThrow(new RuntimeException("Uri failure"));

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals(400, response.getStatus());
  }

  @Test
  void shouldHandleFeignExceptionWithRemoteDetailsButNoCode() {
    feign.FeignException ex = mock(feign.FeignException.class);
    String json = "{\"detail\":\"Remote custom detail\"}"; // No code

    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(json);
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals("Remote custom detail", response.getDetail());
  }

  @Test
  void shouldHandleFeignServerErrorPrefix() {
    feign.FeignException ex = mock(feign.FeignException.class);
    when(ex.status()).thenReturn(503);
    when(ex.getMessage()).thenReturn("Service Unavailable");
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals("Upstream service reported server-side failure", response.getDetail());
  }

  @Test
  void shouldHandleFeignExceptionWithEmptyJsonRemoteDetails() {
    feign.FeignException ex = mock(feign.FeignException.class);
    String json = "{}"; // Valid JSON but empty

    when(ex.status()).thenReturn(400);
    when(ex.contentUTF8()).thenReturn(json);
    when(ex.getMessage()).thenReturn("Standard message");
    when(request.getRequestURI()).thenReturn("/test");

    ProblemDetail response =
        feignExceptionHandler.handleFeignException(ex, request, java.util.Locale.ENGLISH);
    assertEquals("Bad Request", response.getTitle());
    // detail should remain fallback since remote detail is null
    assertEquals(
        "Upstream service reported client-side error: Standard message", response.getDetail());
    // code should remain default (BAD_REQUEST)
    assertEquals("BAD_REQUEST", response.getProperties().get("code"));
  }
}
