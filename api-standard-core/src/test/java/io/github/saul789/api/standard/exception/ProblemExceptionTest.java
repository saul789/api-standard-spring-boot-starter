package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProblemExceptionTest {

  @Test
  void shouldSupportVariousConstructors() {
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
    assertNotNull(new ProblemException("http://custom.com", "msg", HttpStatus.BAD_REQUEST));
    assertNotNull(new ProblemException("msg", HttpStatus.BAD_REQUEST, "http://custom.com"));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (URI) null));
    assertNotNull(new ProblemException("msg", HttpStatus.BAD_GATEWAY));
    assertNotNull(new ProblemException(ErrorCode.BAD_REQUEST, "msg", HttpStatus.BAD_REQUEST, (String) null));
  }
}
