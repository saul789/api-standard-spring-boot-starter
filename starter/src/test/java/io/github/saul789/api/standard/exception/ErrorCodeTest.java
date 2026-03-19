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
                        ErrorCode.NOT_FOUND);
    }

    @Test
    void shouldSupportValueOf() {

        assertThat(ErrorCode.valueOf("INTERNAL_ERROR"))
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}