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
                        ErrorCode.FORBIDDEN);
    }

    @Test
    void shouldSupportValueOf() {

        assertThat(ErrorCode.valueOf("INTERNAL_ERROR"))
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}