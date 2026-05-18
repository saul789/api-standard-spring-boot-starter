package io.github.saul789.api.standard.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.saul789.api.standard.exception.ErrorCode;
import io.github.saul789.api.standard.exception.ProblemDetailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

/**
 * Unit tests for {@link SecurityProblemSupport}.
 */
class SecurityProblemSupportTest {

  private ProblemDetailService problemDetailService;
  private ObjectMapper objectMapper;
  private SecurityProblemSupport securityProblemSupport;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private StringWriter responseBody;

  @BeforeEach
  void setUp() throws Exception {
    problemDetailService = mock(ProblemDetailService.class);
    objectMapper = new ObjectMapper(); // Use real ObjectMapper to avoid null on copy()
    securityProblemSupport = new SecurityProblemSupport(problemDetailService, objectMapper);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);

    responseBody = new StringWriter();
    PrintWriter writer = new PrintWriter(responseBody);
    when(response.getWriter()).thenReturn(writer);
  }

  @Test
  void commence_ShouldCreateProblemDetailAndWriteResponse() throws Exception {
    // Arrange
    AuthenticationException authException = new InsufficientAuthenticationException("Full authentication is required");
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Full authentication is required");

    when(problemDetailService.createProblem(
            eq(HttpStatus.UNAUTHORIZED),
            eq(request),
            eq("Full authentication is required"),
            eq(ErrorCode.UNAUTHORIZED.name()),
            any(),
            any(Locale.class)))
        .thenReturn(problemDetail);

    // Act
    securityProblemSupport.commence(request, response, authException);

    // Assert
    verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
    verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    
    String jsonResponse = responseBody.toString();
    assertTrue(jsonResponse.contains("status"));
    assertTrue(jsonResponse.contains("401"));
    assertTrue(jsonResponse.contains("Full authentication is required"));
  }

  @Test
  void handle_ShouldCreateProblemDetailAndWriteResponse() throws Exception {
    // Arrange
    AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied");

    when(problemDetailService.createProblem(
            eq(HttpStatus.FORBIDDEN),
            eq(request),
            eq("Access is denied"),
            eq(ErrorCode.FORBIDDEN.name()),
            any(),
            any(Locale.class)))
        .thenReturn(problemDetail);

    // Act
    securityProblemSupport.handle(request, response, accessDeniedException);

    // Assert
    verify(response).setStatus(HttpStatus.FORBIDDEN.value());
    verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    
    String jsonResponse = responseBody.toString();
    assertTrue(jsonResponse.contains("status"));
    assertTrue(jsonResponse.contains("403"));
    assertTrue(jsonResponse.contains("Access is denied"));
  }
}
