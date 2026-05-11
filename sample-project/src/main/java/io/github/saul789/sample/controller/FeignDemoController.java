package io.github.saul789.sample.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Demuestra escenarios donde la librería intercepta errores de Feign clients. Si fallamos con un
 * error de un servicio externo, la librería estandariza el error. Los errores 4xx del cliente
 * externo se propagan, los 5xx o desconocidos se transforman en 502 Bad Gateway.
 */
@RestController
@RequestMapping("/api/demo/feign")
public class FeignDemoController {

  /**
   * Simulación de un error 503 (Service Unavailable) de un cliente Feign externo. La respuesta
   * resultante será un 502 Bad Gateway para evitar exponer detalles internos.
   */
  @GetMapping("/upstream-failure")
  public void throwFeignServiceUnavailable() {
    throw new feign.FeignException.ServiceUnavailable(
        "Upstream service down",
        feign.Request.create(
            feign.Request.HttpMethod.GET,
            "/external/api",
            java.util.Collections.emptyMap(),
            null,
            null,
            null),
        null,
        null);
  }

  /**
   * Simulación de un error 404 (Not Found) de un cliente Feign externo. La respuesta resultante
   * será un 404 Not Found para informar al cliente final.
   */
  @GetMapping("/upstream-notfound")
  public void throwFeignNotFound() {
    throw new feign.FeignException.NotFound(
        "Resource not found in remote service",
        feign.Request.create(
            feign.Request.HttpMethod.GET,
            "/remote-resource",
            java.util.Collections.emptyMap(),
            null,
            null,
            null),
        null,
        null);
  }
}
