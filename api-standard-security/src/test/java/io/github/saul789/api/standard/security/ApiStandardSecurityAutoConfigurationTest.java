package io.github.saul789.api.standard.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.saul789.api.standard.exception.ProblemDetailService;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApiStandardSecurityAutoConfiguration}.
 */
class ApiStandardSecurityAutoConfigurationTest {

  @Test
  void securityProblemSupport_ShouldReturnInstance() {
    // Arrange
    ApiStandardSecurityAutoConfiguration configuration = new ApiStandardSecurityAutoConfiguration();
    ProblemDetailService problemDetailService = mock(ProblemDetailService.class);
    ObjectMapper objectMapper = mock(ObjectMapper.class);

    // Act
    SecurityProblemSupport support = configuration.securityProblemSupport(problemDetailService, objectMapper);

    // Assert
    assertNotNull(support);
  }
}
