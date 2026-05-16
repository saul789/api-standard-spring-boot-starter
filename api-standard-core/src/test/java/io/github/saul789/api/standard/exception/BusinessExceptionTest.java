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

  @Test
  void shouldSupportBuilder() {
    java.net.URI uri = java.net.URI.create("http://test.com");

    // code == null, type == null
    BusinessException b1 = BusinessException.builder("Msg").status(HttpStatus.NOT_FOUND).build();
    assertThat(b1.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(b1.getProblemType()).isNull();

    // code == null, type != null
    BusinessException b2 = BusinessException.builder("Msg").status(HttpStatus.BAD_GATEWAY).type("http://type").build();
    assertThat(b2.getCode()).isEqualTo(ErrorCode.BAD_GATEWAY);
    assertThat(b2.getProblemType().toString()).isEqualTo("http://type");

    // code != null, type == null
    BusinessException b3 = BusinessException.builder("Msg").code(ErrorCode.NOT_FOUND).build();
    assertThat(b3.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(b3.getProblemType()).isNull();

    // code != null, type != null
    BusinessException b4 = BusinessException.builder("Msg").code(ErrorCode.NOT_FOUND).type(uri).build();
    assertThat(b4.getCode()).isEqualTo(ErrorCode.NOT_FOUND);
    assertThat(b4.getProblemType().toString()).isEqualTo("http://test.com");

    // type(String) == null
    BusinessException b5 = BusinessException.builder("Msg").type((String) null).build();
    assertThat(b5.getProblemType()).isNull();
  }
}
