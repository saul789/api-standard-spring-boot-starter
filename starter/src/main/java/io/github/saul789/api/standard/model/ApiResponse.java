package io.github.saul789.api.standard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import java.time.Instant;

/**
 * Immutable envelope for all successful API responses.
 *
 * <p>Wraps the actual payload with metadata (timestamp, trace-id, path)
 * to provide a consistent structure across all endpoints. Errors are
 * communicated via RFC 9457 {@code ProblemDetail} instead.
 *
 * <p>Use the factory methods {@link #success} and {@link #error} to
 * construct instances; the constructor is intentionally private.
 *
 * @param <T> the type of the response payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String path;
    private final Instant timestamp;
    private final String traceId;

    private ApiResponse(boolean success,
            String message,
            T data,
            String path,
            String traceId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.path = path;
        this.timestamp = Instant.now();
        this.traceId = traceId;
    }

    /**
     * Creates a successful response wrapping the given payload.
     *
     * @param <T>     the payload type
     * @param data    the response payload; may be {@code null} for empty responses
     * @param message a short, human-readable status description
     * @param path    the request URI that produced this response
     * @return a new {@code ApiResponse} with {@code success = true}
     */
    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return new ApiResponse<>(true, message, data, path, MDC.get("traceId"));
    }

    /**
     * Creates a failed response with no payload.
     *
     * @param <T>     the nominal payload type (always {@code null})
     * @param message a short description of the error
     * @param path    the request URI that produced this response
     * @return a new {@code ApiResponse} with {@code success = false} and no data
     */
    public static <T> ApiResponse<T> error(String message, String path) {
        return new ApiResponse<>(false, message, null, path, MDC.get("traceId"));
    }

    /** Returns {@code true} if the operation succeeded. */
    public boolean isSuccess() {
        return success;
    }

    /** Returns a short, human-readable status description. */
    public String getMessage() {
        return message;
    }

    /** Returns the response payload, or {@code null} for error responses. */
    public T getData() {
        return data;
    }

    /** Returns the request URI that produced this response. */
    public String getPath() {
        return path;
    }

    /** Returns the instant at which this response was created. */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the W3C Trace Context trace-id propagated via MDC, or {@code null}
     * if no tracing context was active.
     */
    public String getTraceId() {
        return traceId;
    }
}