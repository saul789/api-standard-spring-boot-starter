package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception handler that centralizes all error responses in RFC 9457 format (Problem
 * Detail).
 *
 * <p>It provides automatic enrichment of responses with machine-readable error codes, custom
 * problem type URIs (URNs or URLs), and i18n support.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MessageSource messageSource;
  private final ProblemDetailService problemDetailService;

  public GlobalExceptionHandler(
      MessageSource messageSource, ProblemDetailService problemDetailService) {
    this.messageSource = messageSource;
    this.problemDetailService = problemDetailService;
  }

  /** Handles problem-based exceptions (ProblemException and subclasses like BusinessException). */
  @ExceptionHandler(ProblemException.class)
  public ProblemDetail handleProblemException(
      ProblemException ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        ex.getStatus(), request, ex.getMessage(), ex.getCode().name(), ex, locale);
  }

  /** Handles Bean Validation errors in request bodies. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request, Locale locale) {
    ProblemDetail problem =
        problemDetailService.createProblem(
            HttpStatus.BAD_REQUEST,
            request,
            "error.validation.body",
            ErrorCode.VALIDATION_ERROR.name(),
            ex,
            locale);

    List<ValidationError> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                f -> {
                  String message = f.getDefaultMessage();
                  for (String code : f.getCodes()) {
                    String msg = messageSource.getMessage(code, f.getArguments(), null, locale);
                    if (msg != null && !msg.equals(code)) {
                      message = msg;
                      break;
                    }
                  }

                  if (message != null && message.equals(f.getDefaultMessage())) {
                    message = messageSource.getMessage(message, f.getArguments(), message, locale);
                  }

                  return new ValidationError(f.getField(), message);
                })
            .sorted(
                java.util.Comparator.comparing(ValidationError::getField)
                    .thenComparing(ValidationError::getMessage))
            .toList();

    problem.setProperty("errors", errors);
    return problem;
  }

  /** Handles Bean Validation errors in request parameters (URL/Query). */
  @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(
      jakarta.validation.ConstraintViolationException ex,
      HttpServletRequest request,
      Locale locale) {
    ProblemDetail problem =
        problemDetailService.createProblem(
            HttpStatus.BAD_REQUEST,
            request,
            "error.validation.params",
            ErrorCode.VALIDATION_ERROR.name(),
            ex,
            locale);

    List<ValidationError> errors =
        ex.getConstraintViolations().stream()
            .map(
                v -> {
                  String field = "";
                  for (var node : v.getPropertyPath()) {
                    field = node.getName();
                  }
                  String template = v.getMessageTemplate();
                  String message =
                      (template != null && template.startsWith("{") && template.endsWith("}"))
                          ? messageSource.getMessage(
                              template.substring(1, template.length() - 1),
                              null,
                              v.getMessage(),
                              locale)
                          : v.getMessage();
                  return new ValidationError(field, message);
                })
            .sorted(java.util.Comparator.comparing(ValidationError::getField))
            .toList();

    problem.setProperty("errors", errors);
    return problem;
  }

  /** Handles 404 - Not Found errors when no resource is found. */
  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ProblemDetail handleNoResourceFoundException(
      Exception ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.NOT_FOUND, request, "error.not_found", ErrorCode.NOT_FOUND.name(), ex, locale);
  }

  /** Handles 405 - Method Not Allowed errors. */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.METHOD_NOT_ALLOWED,
        request,
        "error.method_not_allowed",
        ErrorCode.METHOD_NOT_ALLOWED.name(),
        ex,
        locale);
  }

  /** Handles 415 - Unsupported Media Type errors. */
  @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
  public ProblemDetail handleMediaTypeNotSupported(
      Exception ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        request,
        "error.unsupported_media_type",
        ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
        ex,
        locale);
  }

  /** Handles Spring's ResponseStatusException and extracts its properties. */
  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatusException(
      ResponseStatusException ex, HttpServletRequest request, Locale locale) {
    return problemDetailService.createProblem(
        HttpStatus.valueOf(ex.getStatusCode().value()),
        request,
        ex.getReason(),
        ErrorCode.fromStatus(ex.getStatusCode().value()).name(),
        ex,
        locale);
  }

  /** Catches any other unhandled exception and converts it to a 500 error. */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(
      Exception ex, HttpServletRequest request, Locale locale) {
    String message = ex.getMessage();
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String codeName = ErrorCode.INTERNAL_ERROR.name();

    // Check Resilience4j exceptions FIRST (they may implement ErrorResponse in integration builds)
    String exceptionClassName = ex.getClass().getName();
    if (exceptionClassName.contains("resilience4j.circuitbreaker.CallNotPermittedException")) {
      return problemDetailService.createProblem(
          HttpStatus.SERVICE_UNAVAILABLE,
          request,
          "error.circuit_breaker_open",
          ErrorCode.SERVICE_UNAVAILABLE.name(),
          ex,
          locale);
    }
    if (exceptionClassName.contains("resilience4j.ratelimiter.RequestNotPermitted")
        || exceptionClassName.contains("resilience4j.bulkhead.BulkheadFullException")) {
      return problemDetailService.createProblem(
          HttpStatus.TOO_MANY_REQUESTS,
          request,
          "error.too_many_requests",
          ErrorCode.TOO_MANY_REQUESTS.name(),
          ex,
          locale);
    }

    if (ex instanceof ErrorResponse errorResponse) {
      ProblemDetail problem = errorResponse.updateAndGetBody(messageSource, locale);
      return problemDetailService.createProblem(
          HttpStatus.valueOf(problem.getStatus()),
          request,
          problem.getDetail(),
          ErrorCode.fromStatus(problem.getStatus()).name(),
          ex,
          locale);
    }

    ResponseStatus responseStatus =
        AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
    if (responseStatus != null) {
      status = responseStatus.code();
      codeName = ErrorCode.fromStatus(status.value()).name();
    } else {
      String exceptionName = ex.getClass().getSimpleName();
      if (exceptionName.contains("AccessDeniedException")) {
        status = HttpStatus.FORBIDDEN;
        codeName = ErrorCode.FORBIDDEN.name();
        message = "error.forbidden";
      } else if (ex instanceof java.util.concurrent.TimeoutException
          || exceptionName.contains("TimeoutException")) {
        status = HttpStatus.GATEWAY_TIMEOUT;
        codeName = ErrorCode.GATEWAY_TIMEOUT.name();
        message = "error.gateway_timeout";
      } else {
        if (log.isErrorEnabled()) {
          log.error("Unhandled exception caught: {}", ex.getMessage(), ex);
        }
        message = "error.internal_error";
      }
    }

    return problemDetailService.createProblem(status, request, message, codeName, ex, locale);
  }
}
