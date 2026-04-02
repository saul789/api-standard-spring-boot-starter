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

/**
 * Controlador de Usuarios auditado que cumple con el estándar RFC 9457.
 */
@RestController
@RequestMapping("/api/demo/users")
public class UserController {

    /**
     * Simula la creación de un usuario.
     * Demuestra:
     * 1. Validación automática (@Valid) -> VALIDATION_ERROR.
     * 2. Regla de negocio (Usuario duplicado) -> CONFLICT.
     * 3. i18n con claves dinámicas.
     */
    @PostMapping
    public String createUser(@Valid @RequestBody UserRequest request) {
        // Simulación: Si el email es test@example.com, lanzamos excepción de negocio
        if ("test@example.com".equalsIgnoreCase(request.email())) {
            throw new BusinessException(
                ErrorCode.CONFLICT, 
                "error.user.already_exists", 
                HttpStatus.CONFLICT,
                "https://api.saul.dev/docs/errors/user-limits"
            );
        }
        
        return "Usuario " + request.name() + " creado correctamente.";
    }
}
