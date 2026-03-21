package io.github.saul789.api.standard.exception;

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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

/**
 * Central exception handler that converts application exceptions into
 * RFC 9457 {@code ProblemDetail} responses.
 *
 * <p>All responses produced here include the following extension properties
 * beyond the RFC minimum:
 * <ul>
 *   <li>{@code code} — a machine-readable {@link ErrorCode} constant</li>
 *   <li>{@code timestamp} — the instant the error was generated</li>
 *   <li>{@code traceId} — the W3C trace-id from MDC, when present</li>
 * </ul>
 *
 * <p>Titles and details are resolved through {@link MessageSource} to support
 * i18n. If a key has no translation the raw key is returned as the detail,
 * and the standard HTTP reason phrase is used as the title.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Handles known business-rule violations.
     *
     * <p>The exception's message is treated as a potential i18n key; if no
     * translation is found the raw value is used as the {@code detail}.
     *
     * @param ex      the business exception carrying the error code and status
     * @param request the current HTTP request
     * @return an RFC 9457 problem detail with the exception's HTTP status
     */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatus());
        enrich(problem, request, ex.getMessage(), ex.getCode().name());
        return problem;
    }

    /**
     * Handles {@link ResponseStatusException} thrown by controllers or filters.
     *
     * @param ex      the exception carrying the HTTP status and optional reason
     * @param request the current HTTP request
     * @return an RFC 9457 problem detail mirroring the exception's status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatusCode());
        enrich(problem, request, ex.getReason(), ErrorCode.BAD_REQUEST.name());
        return problem;
    }

    /**
     * Handles {@code @Valid} / {@code @Validated} body-binding failures.
     *
     * <p>Field-level errors are attached as an {@code "errors"} extension
     * property, each containing the field path and a localised message.
     *
     * @param ex      the validation exception produced by Spring MVC
     * @param request the current HTTP request
     * @return a 400 problem detail with per-field {@link ValidationError} entries
     */
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

    /**
     * Handles Bean Validation constraint violations raised outside a request body
     * (e.g. path variables or service-layer validation).
     *
     * @param ex      the constraint violation exception
     * @param request the current HTTP request
     * @return a 400 problem detail with per-constraint {@link ValidationError} entries
     */
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

    /**
     * Catch-all handler for any unhandled {@link Exception}.
     *
     * <p>The exception is logged at {@code ERROR} level with the request URI
     * to facilitate diagnosis without leaking internal details to the caller.
     *
     * @param ex      the unhandled exception
     * @param request the current HTTP request
     * @return a 500 problem detail
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing request: {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        enrich(problem, request, "error.internal", ErrorCode.INTERNAL_ERROR.name());
        return problem;
    }

    /**
     * Enriches a {@link ProblemDetail} with i18n title/detail and standard
     * extension properties ({@code code}, {@code instance}, {@code timestamp},
     * and optionally {@code traceId}).
     *
     * <p>The title is resolved from the key {@code "error.<CODE>"}; if absent,
     * the standard HTTP reason phrase is used (or {@code "Error"} for unknown
     * status codes). The detail is resolved from {@code detailKey}; if absent,
     * the key itself is returned verbatim so clients can still identify the
     * missing translation.
     *
     * @param problem   the problem detail to enrich (mutated in place)
     * @param request   the current HTTP request, used for the {@code instance} URI
     * @param detailKey an i18n message key or fallback detail string
     * @param code      the {@link ErrorCode} name to embed as {@code "code"}
     */
    private void enrich(ProblemDetail problem, HttpServletRequest request, String detailKey, String code) {
        Locale locale = LocaleContextHolder.getLocale();
        int status = problem.getStatus();

        String defaultTitle = (status >= 100 && status <= 599)
                ? HttpStatus.valueOf(status).getReasonPhrase()
                : "Error";
        problem.setTitle(messageSource.getMessage("error." + code, null, defaultTitle, locale));
        problem.setDetail(messageSource.getMessage(detailKey, null, detailKey, locale));

        problem.setProperty("code", code);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        String traceId = MDC.get("traceId");
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }
}