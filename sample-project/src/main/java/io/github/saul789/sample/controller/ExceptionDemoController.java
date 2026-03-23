package io.github.saul789.sample.controller;

import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Demuestra escenarios donde la librería intercepta excepciones personalizadas
 * o genéricas y devuelve un RFC 9457 ProblemDetail uniforme.
 */
@RestController
@RequestMapping("/api/demo/exceptions")
public class ExceptionDemoController {

    /**
     * Excepción de negocio estándar del starter.
     * Permite especificar el código de error y el estado HTTP de forma sencilla.
     * Resultado: 400 Bad Request con código "BAD_REQUEST".
     */
    @GetMapping("/business-logic")
    public void throwBusiness() {
        throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "Regla de negocio: El usuario alcanzó el límite de solicitudes.",
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Excepción personalizada decorada con @ResponseStatus.
     * La librería ahora respeta estas anotaciones automáticamente sin pasos extra.
     * Resultado: 409 Conflict con código "BAD_REQUEST" (auto-mapeado por rango 4xx).
     */
    @GetMapping("/annotated")
    public void throwAnnotated() {
        throw new CustomConflictException("Este recurso ya está siendo editado por otro usuario.");
    }

    /**
     * Excepción genérica de Spring (ResponseStatusException).
     * Útil para lanzar errores rápidos sin crear clases de excepción.
     * Resultado: 404 Not Found con código "NOT_FOUND" (auto-mapeado).
     */
    @GetMapping("/not-found")
    public void throwResponseStatus() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontré el recurso solicitado.");
    }

    /**
     * Excepción genérica "desconocida" (fallback).
     * Cualquier excepción que no se encuentre en un handler específico se captura aquí.
     * Resultado: 500 Internal Server Error con código "INTERNAL_ERROR".
     */
    @GetMapping("/server-error")
    public void throwGeneric() {
        throw new UnsupportedOperationException("Esta operación aún no está implementada.");
    }

    // Excepción personalizada decorada con @ResponseStatus
    @ResponseStatus(HttpStatus.CONFLICT)
    public static class CustomConflictException extends RuntimeException {
        public CustomConflictException(String message) {
            super(message);
        }
    }
}
