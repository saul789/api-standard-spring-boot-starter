package io.github.saul789.api.standard.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

  @Test
  void shouldStoreCodeMessageAndStatus() {

    ErrorCode code = ErrorCode.BAD_REQUEST;
    String message = "Invalid operation";
    HttpStatus status = HttpStatus.BAD_REQUEST;

    BusinessException exception = new BusinessException(code, message, status);

    assertThat(exception.getCode()).isEqualTo(code);
    assertThat(exception.getStatus()).isEqualTo(status);
    assertThat(exception.getMessage()).isEqualTo(message);
  }
}
