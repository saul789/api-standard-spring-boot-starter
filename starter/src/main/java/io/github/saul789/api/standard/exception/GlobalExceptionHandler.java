package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.model.ApiResponse;
import io.github.saul789.api.standard.model.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

/**
 * Global exception handler based on RFC 9457 (Problem Details for HTTP APIs).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatus());
        // Pasamos el mensaje de la excepción como posible llave de traducción
        enrich(problem, request, ex.getMessage(), ex.getCode().name());
        return problem;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
        enrich(problem, request, ex.getReason(), ErrorCode.BAD_REQUEST.name());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        enrich(problem, request, "error.validation.body", ErrorCode.VALIDATION_ERROR.name());

        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        messageSource.getMessage(error, LocaleContextHolder.getLocale())))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        enrich(problem, request, "error.validation.params", ErrorCode.VALIDATION_ERROR.name());

        var errors = ex.getConstraintViolations()
                .stream()
                .map(v -> new ValidationError(
                        v.getPropertyPath().toString(),
                        v.getMessage()))
                .toList();

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        enrich(problem, request, "error.internal", ErrorCode.INTERNAL_ERROR.name());
        return problem;
    }

    /**
     * Enriches ProblemDetail following RFC 9457 and providing i18n support.
     */
    private void enrich(ProblemDetail problem, HttpServletRequest request, String detailKey, String code) {
        Locale locale = LocaleContextHolder.getLocale();
        int status = problem.getStatus();

        // 1. Título traducido: Busca 'error.BAD_REQUEST', etc. Si no existe, usa el
        // motivo HTTP estándar.
        String defaultTitle = (status >= 100 && status <= 599)
                ? HttpStatus.valueOf(status).getReasonPhrase()
                : "Error";
        problem.setTitle(messageSource.getMessage("error." + code, null, defaultTitle, locale));

        // 2. Detalle traducido: Intenta traducir la llave recibida.
        problem.setDetail(messageSource.getMessage(detailKey, null, detailKey, locale));

        // 3. Extensiones (RFC 9457 permite miembros adicionales)
        problem.setProperty("code", code);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleFeignException(feign.FeignException ex,
            HttpServletRequest request) {
        int externalStatus = ex.status();
        HttpStatus responseStatus;
        String errorMessage;

        // Lógica de mapeo: 4xx se propaga, 5xx se convierte en 502
        if (externalStatus >= 400 && externalStatus < 500) {
            responseStatus = HttpStatus.resolve(externalStatus);
            if (responseStatus == null)
                responseStatus = HttpStatus.BAD_REQUEST;
            errorMessage = "External client error: " + ex.getMessage();
        } else {
            // El 500 del vecino es mi 502
            responseStatus = HttpStatus.BAD_GATEWAY;
            errorMessage = "External service failure (Upstream error)";
        }

        log.error("Feign communication failed. External Status: {}, Target Path: {}",
                externalStatus, request.getRequestURI());

        ApiResponse<Object> errorResponse = ApiResponse.error(
                errorMessage,
                request.getRequestURI());

        return ResponseEntity.status(responseStatus).body(errorResponse);
    }
}