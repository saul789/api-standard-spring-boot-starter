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
    assertTrue(response.containsKey("starter"));
    assertTrue(response.containsKey("degradedMode"));
    assertTrue(response.containsKey("redaction"));
    assertTrue(response.containsKey("errorCodes"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> starter = (Map<String, Object>) response.get("starter");
    assertEquals("io.github.saul789.api.standard", starter.get("basePackage"));
  }
}
