package io.github.saul789.api.standard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.saul789.api.standard.exception.ProblemDetailService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.AuthenticationEntryPoint;

@AutoConfiguration
@ConditionalOnClass(AuthenticationEntryPoint.class)
public class ApiStandardSecurityAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public SecurityProblemSupport securityProblemSupport(
      ProblemDetailService problemDetailService, ObjectMapper objectMapper) {
    return new SecurityProblemSupport(problemDetailService, objectMapper);
  }
}
