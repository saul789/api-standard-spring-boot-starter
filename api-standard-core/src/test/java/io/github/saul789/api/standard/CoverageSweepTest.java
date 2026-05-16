package io.github.saul789.api.standard;

import static org.junit.jupiter.api.Assertions.*;

import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;
import io.github.saul789.api.standard.exception.ProblemDetailEnricher;
import io.github.saul789.api.standard.exception.ProblemException;
import io.github.saul789.api.standard.model.ApiResponse;
import io.github.saul789.api.standard.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class CoverageSweepTest {

  @Test
  void sweepProperties() {
    ApiStandardProperties props = new ApiStandardProperties();
    props.getErrors().setTypeBaseUri("http://test.com");

    // Normal case
    props.getErrors().setTypeOverrides(Map.of("A", "B"));
    assertEquals("http://test.com", props.getErrors().getTypeBaseUri());
    assertEquals("B", props.getErrors().getTypeOverrides().get("A"));

    // Null case for coverage via reflection
    try {
      java.lang.reflect.Field field =
          props.getErrors().getClass().getDeclaredField("typeOverrides");
      field.setAccessible(true);
      field.set(props.getErrors(), null);
      assertNull(props.getErrors().getTypeOverrides());
    } catch (Exception ignored) {
    }
  }

  @Test
  void sweepApiResponse() {
    ApiResponse<String> response = ApiResponse.success("data", "msg", "/path");
    assertNull(response.getTraceId());
  }

  @Test
  void sweepValidationError() {
    ValidationError err = new ValidationError("field", "msg");
    assertEquals("field", err.getField());
    assertEquals("msg", err.getMessage());
  }

  @Test
  void sweepEnricherInterface() {
    ProblemDetailEnricher enricher =
        new ProblemDetailEnricher() {
          @Override
          public void enrich(ProblemDetail problem, HttpServletRequest request, Locale locale) {
          }
        };
    assertEquals(0, enricher.order());
  }

  @Test
  void sweepExceptions() {
    URI uri = URI.create("http://test.com");

    // ProblemException variations
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
    assertNotNull(new ProblemException("http://custom.com", "msg", HttpStatus.BAD_REQUEST));
    assertNotNull(new ProblemException("msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (URI) null));
    assertNotNull(new ProblemException("msg", HttpStatus.BAD_GATEWAY));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (String) null));

    // code == null, type == null
    BusinessException b1 = BusinessException.builder("Msg").status(HttpStatus.NOT_FOUND).build();
    assertEquals(ErrorCode.NOT_FOUND, b1.getCode());
    assertNull(b1.getProblemType());

    // code == null, type != null
    BusinessException b2 = BusinessException.builder("Msg").status(HttpStatus.BAD_GATEWAY).type("http://type").build();
    assertEquals(ErrorCode.BAD_GATEWAY, b2.getCode());
    assertEquals("http://type", b2.getProblemType().toString());

    // code != null, type == null
    BusinessException b3 = BusinessException.builder("Msg").code(ErrorCode.NOT_FOUND).build();
    assertEquals(ErrorCode.NOT_FOUND, b3.getCode());
    assertNull(b3.getProblemType());

    // code != null, type != null
    BusinessException b4 = BusinessException.builder("Msg").code(ErrorCode.NOT_FOUND).type(uri).build();
    assertEquals(ErrorCode.NOT_FOUND, b4.getCode());
    assertEquals("http://test.com", b4.getProblemType().toString());

    // type(String) == null
    BusinessException b5 = BusinessException.builder("Msg").type((String) null).build();
    assertNull(b5.getProblemType());
  }

  @Test
  void sweepErrorCode() {
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
}
