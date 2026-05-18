package io.github.saul789.api.standard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.saul789.api.standard.exception.ErrorCode;
import io.github.saul789.api.standard.exception.ProblemDetailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Bridges Spring Security exceptions to RFC 9457 Problem Details.
 */
public class SecurityProblemSupport implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ProblemDetailService problemDetailService;
  private final ObjectMapper objectMapper;

  public SecurityProblemSupport(ProblemDetailService problemDetailService, ObjectMapper objectMapper) {
    this.problemDetailService = Objects.requireNonNull(problemDetailService, "problemDetailService must not be null");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null").copy();
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {

    ProblemDetail detail =
        problemDetailService.createProblem(
            HttpStatus.UNAUTHORIZED,
            request,
            authException.getMessage(),
            ErrorCode.UNAUTHORIZED.name(),
            null,
            Locale.getDefault());

    writeResponse(response, detail);
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {

    ProblemDetail detail =
        problemDetailService.createProblem(
            HttpStatus.FORBIDDEN,
            request,
            accessDeniedException.getMessage(),
            ErrorCode.FORBIDDEN.name(),
            null,
            Locale.getDefault());

    writeResponse(response, detail);
  }

  private void writeResponse(HttpServletResponse response, ProblemDetail detail)
      throws IOException {
    response.setStatus(detail.getStatus());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getWriter(), detail);
  }
}
