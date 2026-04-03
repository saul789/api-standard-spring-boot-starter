package io.github.saul789.api.standard.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    @Test
    void shouldContainAllExpectedValues() {

        ErrorCode[] values = ErrorCode.values();

        assertThat(values)
                .containsExactlyInAnyOrder(
                        ErrorCode.INTERNAL_ERROR,
                        ErrorCode.VALIDATION_ERROR,
                        ErrorCode.BAD_REQUEST,
                        ErrorCode.NOT_FOUND,
                        ErrorCode.METHOD_NOT_ALLOWED,
                        ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                        ErrorCode.UNAUTHORIZED,
                        ErrorCode.FORBIDDEN,
                        ErrorCode.CONFLICT,
                        ErrorCode.GONE,
                        ErrorCode.TOO_MANY_REQUESTS,
                        ErrorCode.BAD_GATEWAY,
                        ErrorCode.SERVICE_UNAVAILABLE,
                        ErrorCode.GATEWAY_TIMEOUT);
    }

    @Test
    void shouldSupportValueOf() {
        assertThat(ErrorCode.valueOf("INTERNAL_ERROR"))
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void shouldMapStatusToErrorCode() {
        assertThat(ErrorCode.fromStatus(400)).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ErrorCode.fromStatus(404)).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorCode.fromStatus(500)).isEqualTo(ErrorCode.INTERNAL_ERROR);
        
        // Edge cases for branch coverage in default -> clause
        assertThat(ErrorCode.fromStatus(418)).isEqualTo(ErrorCode.BAD_REQUEST); // 4xx default
        assertThat(ErrorCode.fromStatus(501)).isEqualTo(ErrorCode.INTERNAL_ERROR); // Other default
        assertThat(ErrorCode.fromStatus(200)).isEqualTo(ErrorCode.INTERNAL_ERROR); // Unexpected success status
    }

    @Test
    void shouldConvertToKebabCase() {
        assertThat(ErrorCode.INTERNAL_ERROR.toKebabCase()).isEqualTo("internal-error");
        assertThat(ErrorCode.GONE.toKebabCase()).isEqualTo("gone");
        assertThat(ErrorCode.BAD_REQUEST.toKebabCase()).isEqualTo("bad-request");
    }
}