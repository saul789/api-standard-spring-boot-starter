package io.github.saul789.api.standard.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  @Test
  void shouldCreateSuccessResponse() {
    // Preparación
    String data = "Test Data";
    String message = "Success Message";
    String path = "/api/test";

    // Ejecución
    ApiResponse<String> response = ApiResponse.success(data, message, path);

    // Verificación de Getters y Factory (Cubre Constructor y Getters)
    assertTrue(response.isSuccess());
    assertEquals(data, response.getData());
    assertEquals(message, response.getMessage());
    assertEquals(path, response.getPath());
    assertNotNull(response.getTimestamp());
    // Verificamos que el timestamp sea reciente
    assertTrue(response.getTimestamp().isBefore(Instant.now().plusSeconds(1)));
  }

  @Test
  void shouldCreateErrorResponse() {
    // Preparación
    String message = "Error Message";
    String path = "/api/error";

    // Ejecución
    ApiResponse<Object> response = ApiResponse.error(message, path);

    // Verificación
    assertFalse(response.isSuccess());
    assertNull(response.getData());
    assertEquals(message, response.getMessage());
    assertEquals(path, response.getPath());
    assertNotNull(response.getTimestamp());
  }
}
