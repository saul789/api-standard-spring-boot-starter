package io.github.saul789.api.standard.actuator;

import static org.junit.jupiter.api.Assertions.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiErrorsEndpointTest {

  @Test
  void shouldReturnApiErrors() {
    ApiStandardProperties props = new ApiStandardProperties();
    ApiErrorsEndpoint endpoint = new ApiErrorsEndpoint(props);
    
    Map<String, Object> response = endpoint.getApiErrors();
    
    assertNotNull(response);
    assertTrue(response.containsKey("basePackage"));
    assertTrue(response.containsKey("errorCodes"));
  }
}
