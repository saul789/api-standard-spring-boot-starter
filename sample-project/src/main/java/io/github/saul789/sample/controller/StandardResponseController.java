package io.github.saul789.sample.controller;

import io.github.saul789.sample.dto.UserDto;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demuestra escenarios donde la librería envuelve automáticamente las respuestas exitosas. Todas
 * las respuestas (excepto String y byte[]) se envuelven en un objeto ApiResponse.
 */
@RestController
@RequestMapping("/api/demo/responses")
public class StandardResponseController {

  /**
   * Respuesta simple de un objeto DTO. Se convertirá en { "success": true, "data": { ... },
   * "timestamp": "..." }
   */
  @GetMapping("/object")
  public UserDto getObject() {
    return new UserDto("saul789", 30);
  }

  /** Respuesta de una lista de objetos. */
  @GetMapping("/list")
  public List<UserDto> getList() {
    return List.of(new UserDto("alice", 25), new UserDto("bob", 28));
  }

  /** Respuesta de un Map (clave-valor). */
  @GetMapping("/map")
  public Map<String, Object> getMap() {
    return Map.of(
        "message", "Hello from sample project!",
        "status", "UP");
  }

  /**
   * Demostración de respuesta CRUDA (String). Los tipos básicos como String no se envuelven por
   * diseño (ResponseAdvice exclusion).
   */
  @GetMapping("/raw")
  public String getRaw() {
    return "Este texto viaja sin envoltura (es un String plano)";
  }
}
