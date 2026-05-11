package io.github.saul789.api.standard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.saul789.api.standard.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class GlobalExceptionHandlerTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(ApiStandardAutoConfiguration.class));

  @Test
  void shouldRegisterExceptionHandler() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
        });
  }
}
