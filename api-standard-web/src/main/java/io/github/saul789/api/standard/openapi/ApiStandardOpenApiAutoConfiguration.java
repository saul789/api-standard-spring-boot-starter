package io.github.saul789.api.standard.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally loads the OpenAPI customizer only if springdoc-openapi is present in the classpath.
 */
@Configuration
@ConditionalOnClass({OpenAPI.class, OpenApiCustomizer.class})
public class ApiStandardOpenApiAutoConfiguration {

  @Bean
  public OpenApiCustomizer apiStandardOpenApiCustomizer() {
    return new ApiStandardOpenApiCustomizer();
  }
}
