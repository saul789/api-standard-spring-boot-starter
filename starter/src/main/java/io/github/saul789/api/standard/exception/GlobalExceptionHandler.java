package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import io.github.saul789.api.standard.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Global exception handler that centralizes all error responses in RFC 9457 format (Problem Detail).
 *
 * <p>It provides automatic enrichment of responses with machine-readable error codes,
 * custom problem type URIs (URNs or URLs), and i18n support.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;
    private final ApiStandardProperties properties;

    public GlobalExceptionHandler(MessageSource messageSource, ApiStandardProperties properties) {
        this.messageSource = messageSource;
        this.properties = properties;
    }

    /**
     * Handles problem-based exceptions (ProblemException and subclasses like BusinessException).
     */
    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatus());
        enrich(problem, request, ex.getMessage(), ex.getCode().name(), ex.getProblemType());
        return problem;
    }

    /**
     * Handles Bean Validation errors in request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        enrich(problem, request, "error.validation.body", ErrorCode.VALIDATION_ERROR.name(), null);

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ValidationError(f.getField(), f.getDefaultMessage()))
                .toList();
        problem.setProperty("errors", errors);

        return problem;
    }

    /**
     * Handles Bean Validation errors in request parameters.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(jakarta.validation.ConstraintViolationException ex,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        enrich(problem, request, "error.validation.params", ErrorCode.VALIDATION_ERROR.name(), null);

        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String field = "";
                    for (var node : v.getPropertyPath()) {
                        field = node.getName();
                    }
                    return new ValidationError(field, v.getMessage());
                })
                .toList();
        problem.setProperty("errors", errors);

        return problem;
    }

    /**
     * Handles 404 - Not Found errors when no resource is found.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFoundException(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        enrich(problem, request, "error.not_found", ErrorCode.NOT_FOUND.name(), null);
        return problem;
    }

    /**
     * Handles 405 - Method Not Allowed errors.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
        enrich(problem, request, "error.method_not_allowed", ErrorCode.METHOD_NOT_ALLOWED.name(), null);
        return problem;
    }

    /**
     * Handles 415 - Unsupported Media Type errors.
     */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        enrich(problem, request, "error.unsupported_media_type", ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(), null);
        return problem;
    }

    /**
     * Handles Spring's ResponseStatusException and extracts its properties.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail problem = ex.getBody();
        enrich(problem, request, ex.getReason(), ErrorCode.fromStatus(ex.getStatusCode().value()).name(), null);
        return problem;
    }

    /**
     * Catches any other unhandled exception and converts it to a 500 error.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String codeName = ErrorCode.INTERNAL_ERROR.name();
        URI customType = extractCustomType(ex);

        if (ex instanceof ErrorResponse errorResponse) {
            ProblemDetail problem = errorResponse.updateAndGetBody(messageSource, LocaleContextHolder.getLocale());
            enrich(problem, request, problem.getDetail(), ErrorCode.fromStatus(problem.getStatus()).name(), customType);
            return problem;
        }

        ResponseStatus responseStatus = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            status = responseStatus.code();
            codeName = ErrorCode.fromStatus(status.value()).name();
        } else {
            String exceptionName = ex.getClass().getSimpleName();
            if (exceptionName.contains("AccessDeniedException")) {
                status = HttpStatus.FORBIDDEN;
                codeName = ErrorCode.FORBIDDEN.name();
                message = "error.forbidden";
            } else {
                log.error("Unhandled exception caught: {}", ex.getMessage(), ex);
                message = "error.internal_error";
            }
        }

        ProblemDetail problem = ProblemDetail.forStatus(status);
        enrich(problem, request, message, codeName, customType);
        return problem;
    }

    private URI extractCustomType(Exception ex) {
        if (ex instanceof ProblemTypeProvider provider) {
            URI type = provider.getProblemType();
            if (type != null) return type;
        }
        ProblemType annotation = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ProblemType.class);
        if (annotation != null) {
            try { return URI.create(annotation.value()); } catch (Exception _) {}
        }
        return null;
    }

    private void enrich(ProblemDetail problem, HttpServletRequest request, String detailKey, String code, URI customType) {
        Locale locale = LocaleContextHolder.getLocale();
        String defaultTitle;
        try { defaultTitle = HttpStatus.valueOf(problem.getStatus()).getReasonPhrase(); } catch (Exception _) { defaultTitle = "Error"; }
        problem.setTitle(messageSource.getMessage("error." + code, null, defaultTitle, locale));
        problem.setDetail(messageSource.getMessage(detailKey, null, detailKey, locale));
        problem.setProperty("code", code);
        if (customType != null) {
            problem.setType(customType);
        } else if (properties.getErrors().getTypeOverrides().containsKey(code)) {
            try { problem.setType(URI.create(properties.getErrors().getTypeOverrides().get(code))); } catch (Exception _) { problem.setType(generateDefaultType(code)); }
        } else {
            problem.setType(generateDefaultType(code));
        }
        try { problem.setInstance(URI.create(request.getRequestURI())); } catch (Exception _) {}
        problem.setProperty("timestamp", Instant.now());
        String traceId = MDC.get("traceId");
        if (traceId != null) { problem.setProperty("traceId", traceId); }
    }

    private URI generateDefaultType(String code) {
        String baseUri = properties.getErrors().getTypeBaseUri();
        String typeSuffix;
        try { typeSuffix = ErrorCode.valueOf(code).toKebabCase(); } catch (Exception _) { typeSuffix = code.toLowerCase().replace('_', '-'); }
        return URI.create(baseUri + typeSuffix);
    }
}