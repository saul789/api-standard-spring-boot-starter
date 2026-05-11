package io.github.saul789.api.standard.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles exceptions thrown by Resilience4j components (CircuitBreaker, RateLimiter, Bulkhead).
 * This advice is only active if Resilience4j is present on the classpath. It is ordered before
 * GlobalExceptionHandler to take precedence.
 */
@RestControllerAdvice
@ConditionalOnClass(CallNotPermittedException.class)
@Order(1)
public class ResilienceExceptionHandler {

  private final ProblemDetailService problemDetailService;

  public ResilienceExceptionHandler(ProblemDetailService problemDetailService) {
    this.problemDetailService = problemDetailService;
  }

  @ExceptionHandler(CallNotPermittedException.class)
  public ProblemDetail handleCallNotPermittedException(
      CallNotPermittedException ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.SERVICE_UNAVAILABLE,
        request,
        "error.circuit_breaker_open",
        ErrorCode.SERVICE_UNAVAILABLE.name(),
        ex,
        locale);
  }

  @ExceptionHandler({RequestNotPermitted.class, BulkheadFullException.class})
  public ProblemDetail handleRateLimitingExceptions(
      Exception ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.TOO_MANY_REQUESTS,
        request,
        "error.too_many_requests",
        ErrorCode.TOO_MANY_REQUESTS.name(),
        ex,
        locale);
  }
}
