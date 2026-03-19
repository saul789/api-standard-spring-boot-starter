package io.github.saul789.api.standard.model;

import java.time.Instant;

/**
 * Standard API response wrapper.
 */
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String path;
    private final Instant timestamp;

    private ApiResponse(boolean success,
                        String message,
                        T data,
                        String path) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.path = path;
        this.timestamp = Instant.now();
    }

    // =========================
    // Factory methods
    // =========================

    public static <T> ApiResponse<T> success(T data, String message, String path) {
        return new ApiResponse<>(true, message, data, path);
    }

    public static <T> ApiResponse<T> error(String message, String path) {
        return new ApiResponse<>(false, message, null, path);
    }

    // =========================
    // Getters
    // =========================

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}