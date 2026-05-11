package io.github.saul789.sample.controller;

import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;
import io.github.saul789.sample.dto.UserRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador de Usuarios auditado que cumple con el estándar RFC 9457. */
@RestController
@RequestMapping("/api/demo/users")
public class UserController {

  /**
   * Simulates the creation of a user. Demonstrates: 1. Automatic validation (@Valid) ->
   * VALIDATION_ERROR. 2. Business rule (Duplicate user) -> CONFLICT. 3. i18n with dynamic keys.
   */
  @PostMapping
  public String createUser(@Valid @RequestBody UserRequest request) {
    // Simulation: If email is test@example.com, throw a business exception
    if ("test@example.com".equalsIgnoreCase(request.email())) {
      throw new BusinessException(
          ErrorCode.CONFLICT,
          "error.user.already_exists",
          HttpStatus.CONFLICT,
          "https://api.saul.dev/docs/errors/user-limits");
    }

    return "User " + request.name() + " successfully created.";
  }
}
