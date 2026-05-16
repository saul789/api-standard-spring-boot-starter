package io.github.saul789.api.standard;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiStandardPropertiesTest {

  @Test
  void shouldStoreProperties() {
    ApiStandardProperties props = new ApiStandardProperties();
    props.getErrors().setTypeBaseUri("http://test.com");

    // Normal case
    props.getErrors().setTypeOverrides(Map.of("A", "B"));
    assertEquals("http://test.com", props.getErrors().getTypeBaseUri());
    assertEquals("B", props.getErrors().getTypeOverrides().get("A"));
  }
}
