package io.github.saul789.sample.controller;

import io.github.saul789.sample.dto.UserDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Demuestra escenarios donde la librería intercepta errores de validación de JSR-303/Bean
 * Validation y devuelve un ProblemDetail detallado.
 */
@RestController
@RequestMapping("/api/demo/validation")
@Validated // Necesario para validación de parámetros individuales (path/query)
public class ValidationDemoController {

  /**
   * Errores en @RequestBody (Spring MethodArgumentNotValidException). El Detail de la respuesta
   * incluirá el campo problemático y el mensaje. Intenta pasar un nombre vacío en un objeto JSON.
   */
  @PostMapping("/body")
  public UserDto testBodyValidation(@Valid @RequestBody UserDto dto) {
    return dto;
  }

  /**
   * Errores en @RequestParam o @PathVariable (ConstraintViolationException). El Detail de la
   * respuesta incluirá la lista de violaciones en formato estandarizado. Intenta pasar un 'code'
   * corto (ej. ?code=a).
   */
  @GetMapping("/params")
  public String testParamValidation(
      @RequestParam @Size(min = 3, message = "El código debe tener al menos 3 caracteres")
          String code) {
    return "Parámetro válido: " + code;
  }

  /**
   * Errores en Path Variable. Intenta pasar un usuario con id vacío (aunque Spring suele fallar en
   * match antes).
   */
  @GetMapping("/{userId}")
  public String testPathValidation(@PathVariable @NotBlank String userId) {
    return "Usuario: " + userId;
  }
}
