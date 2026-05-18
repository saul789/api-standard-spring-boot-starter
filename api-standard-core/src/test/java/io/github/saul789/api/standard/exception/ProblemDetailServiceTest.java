package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.saul789.api.standard.ApiStandardProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemDetailServiceTest {

  private MessageSource messageSource;
  private ApiStandardProperties properties;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    messageSource = mock(MessageSource.class);
    when(messageSource.getMessage(anyString(), any(), anyString(), any()))
        .thenAnswer(invocation -> invocation.getArgument(2));
    properties = new ApiStandardProperties();
    request = mock(HttpServletRequest.class);
  }

  @Test
  void shouldCreateProblemSuccessfully() {
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(
            HttpStatus.BAD_REQUEST, request, "detail_message", "BAD_REQUEST", null, Locale.ENGLISH);

    assertEquals(400, result.getStatus());
    assertEquals("BAD_REQUEST", result.getProperties().get("code"));
    assertEquals("detail_message", result.getDetail());
    assertEquals("urn:problem-type:bad-request", result.getType().toString());
  }

  @Test
  void shouldHandleProblemTypeProviderException() {
    class ProviderException extends RuntimeException implements ProblemTypeProvider {
      @Override
      public URI getProblemType() {
        return URI.create("http://provider.com");
      }
    }
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            request,
            "msg",
            "INTERNAL_ERROR",
            new ProviderException(),
            Locale.ENGLISH);
    assertEquals("http://provider.com", result.getType().toString());
  }

  @Test
  void shouldHandleAnnotatedProblemTypeException() {
    @ProblemType("http://annotated.com")
    class AnnotatedEx extends RuntimeException {}
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            request,
            "msg",
            "INTERNAL_ERROR",
            new AnnotatedEx(),
            Locale.ENGLISH);
    assertEquals("http://annotated.com", result.getType().toString());
  }

  @Test
  void shouldHandleProblemDetailServiceReflection() throws Exception {
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    java.lang.reflect.Method resolveType =
        ProblemDetailService.class.getDeclaredMethod("resolveType", String.class, URI.class);
    resolveType.setAccessible(true);

    // Case: null code
    Object resultNull = resolveType.invoke(service, null, null);
    assertEquals("urn:problem-type:unknown-error", resultNull.toString());

    // Case: blank code
    Object resultBlank = resolveType.invoke(service, "  ", null);
    assertEquals("urn:problem-type:unknown-error", resultBlank.toString());

    // Case: not an ErrorCode
    Object resultInvalid = resolveType.invoke(service, "CUSTOM_ERROR", null);
    assertEquals("urn:problem-type:custom-error", resultInvalid.toString());
  }

  @Test
  void shouldHandleExtractCustomTypeEdgeCases() throws Exception {
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    java.lang.reflect.Method extractType =
        ProblemDetailService.class.getDeclaredMethod("extractCustomType", Exception.class);
    extractType.setAccessible(true);

    // Case: null exception
    assertNull(extractType.invoke(service, (Object) null));

    // Case: not annotated
    assertNull(extractType.invoke(service, new RuntimeException()));

    // Case: annotated with invalid URI
    @ProblemType("invalid uri with spaces")
    class InvalidAnnotatedEx extends RuntimeException {}
    assertNull(extractType.invoke(service, new InvalidAnnotatedEx()));
  }

  @Test
  void shouldHandleInvalidTypeOverrideGracefully() {
    properties.getErrors().setTypeOverrides(Map.of("BAD_REQUEST", "invalid uri with spaces"));
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(
            HttpStatus.BAD_REQUEST, request, "detail", "BAD_REQUEST", null, Locale.ENGLISH);
    assertEquals("urn:problem-type:bad-request", result.getType().toString());
  }

  @Test
  void shouldHandleValidTypeOverrideInService() {
    properties.getErrors().setTypeOverrides(Map.of("CUSTOM_CODE", "http://valid-override.com"));
    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(
            HttpStatus.BAD_REQUEST, request, "detail", "CUSTOM_CODE", null, Locale.ENGLISH);
    assertEquals("http://valid-override.com", result.getType().toString());
  }

  @Test
  void shouldHandleStatusReasonPhraseFailure() {
    HttpStatus status = mock(HttpStatus.class);
    when(status.value()).thenReturn(500);
    when(status.getReasonPhrase()).thenThrow(new RuntimeException("Simulation"));

    ProblemDetailService service = new ProblemDetailService(messageSource, properties, List.of());
    ProblemDetail result =
        service.createProblem(status, request, "detail", "INTERNAL_ERROR", null, Locale.ENGLISH);
    assertEquals(500, result.getStatus());
    assertEquals("Error", result.getTitle());
  }
}
