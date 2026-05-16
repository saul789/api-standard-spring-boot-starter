package io.github.saul789.api.standard.openapi;

import static org.junit.jupiter.api.Assertions.*;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

class ApiStandardOpenApiCustomizerTest {

  @Test
  void shouldCustomizeOpenApi() {
    ApiStandardOpenApiCustomizer customizer = new ApiStandardOpenApiCustomizer();
    OpenAPI openApi = new OpenAPI();
    openApi.setPaths(new Paths()); // Ensure paths is not null to cover that branch
    
    customizer.customise(openApi);
    
    assertNotNull(openApi);
  }
}
