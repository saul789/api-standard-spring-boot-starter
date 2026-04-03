package io.github.saul789.api.standard.advice;

import io.github.saul789.api.standard.model.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Wraps successful controller return values in an {@link ApiResponse} envelope.
 *
 * <p>This advice intercepts every response body before serialisation. Responses
 * that are already an {@link ApiResponse} or an RFC 9457 {@link ProblemDetail}
 * are passed through untouched. Raw {@code String} and {@code byte[]} returns,
 * as well as Spring {@code Resource} streams, are also excluded to avoid
 * corrupting plain-text or binary payloads.
 *
 * <p>The current request URI is read from the injected
 * {@link HttpServletRequest} (request-scoped proxy) so the path is always
 * accurate even in async contexts.
 */
@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final HttpServletRequest request;

    public ApiResponseAdvice(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Determines whether this advice should apply to a given return type.
     *
     * <p>Returns {@code false} for {@code String}, {@code byte[]}, and any
     * subtype of {@link org.springframework.core.io.Resource} so that raw
     * and binary responses are never wrapped.
     *
     * @param returnType    the controller method's return type descriptor
     * @param converterType the selected message converter (unused)
     * @return {@code true} if the response body should be wrapped
     */
    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {

        return Optional.ofNullable(returnType.getParameterType())
                .map(type -> !String.class.equals(type) &&
                        !byte[].class.equals(type) &&
                        !org.springframework.core.io.Resource.class.isAssignableFrom(type))
                .orElse(false);
    }

    /**
     * Wraps the body in an {@link ApiResponse} unless it is already an
     * {@link ApiResponse} or a {@link ProblemDetail}.
     *
     * @param body                  the value returned by the controller
     * @param returnType            the controller method's return type descriptor
     * @param selectedContentType   the negotiated content type (unused)
     * @param selectedConverterType the selected converter (unused)
     * @param request               the server-side HTTP request (unused; URI is
     *                              read from the injected servlet request)
     * @param response              the server-side HTTP response (unused)
     * @return the original body or a new {@link ApiResponse} wrapping it
     */
    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            org.springframework.http.server.ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response) {

        if (body instanceof ProblemDetail) {
            return body;
        }

        if (body instanceof ApiResponse<?>) {
            return body;
        }

        return ApiResponse.success(
                body,
                "OK",
                this.request.getRequestURI());
    }
}