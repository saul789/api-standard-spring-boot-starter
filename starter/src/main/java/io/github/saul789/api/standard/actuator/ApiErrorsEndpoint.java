package io.github.saul789.api.standard.actuator;

import io.github.saul789.api.standard.ApiStandardProperties;
import io.github.saul789.api.standard.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Spring Boot Actuator endpoint that exposes all registered {@link ErrorCode}s and their
 * configurations. Accessible at GET /actuator/api-errors
 */
@Endpoint(id = "api-errors")
public class ApiErrorsEndpoint {

  private final ApiStandardProperties properties;

  public ApiErrorsEndpoint(ApiStandardProperties properties) {
    this.properties = properties;
  }

  @ReadOperation
  public Map<String, Object> getApiErrors() {
    List<Map<String, Object>> codes =
        Arrays.stream(ErrorCode.values())
            .map(
                code -> {
                  String name = code.name();
                  String kebabCase = code.toKebabCase();
                  String typeOverride = properties.getErrors().getTypeOverrides().get(name);

                  return Map.<String, Object>of(
                      "name", name,
                      "kebabCase", kebabCase,
                      "typeUri",
                          typeOverride != null ? typeOverride : "urn:problem-type:" + kebabCase);
                })
            .toList();

    return Map.of("basePackage", "io.github.saul789.api.standard", "errorCodes", codes);
  }
}
