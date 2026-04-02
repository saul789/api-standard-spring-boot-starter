package io.github.saul789.sample.controller;

import io.github.saul789.api.standard.exception.BusinessException;
import io.github.saul789.api.standard.exception.ErrorCode;
import io.github.saul789.api.standard.exception.ProblemException;
import io.github.saul789.sample.exception.CustomConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller demonstrating various error scenarios and how they are handled by the API standard.
 */
@RestController
@RequestMapping("/api/demo/exceptions")
public class ExceptionDemoController {

    /**
     * Usa la URN estandar generada automáticamente por la librería.
     */
    @GetMapping("/problem-internal")
    public void throwInternal() {
        // Generará: urn:problem-type:too-many-requests
        throw new ProblemException("Límite de cuota alcanzado.", HttpStatus.TOO_MANY_REQUESTS);
    }

    /**
     * Muestra cómo el cliente puede proporcionar su propia URL para anular la URN estándar.
     */
    @GetMapping("/problem-external")
    public void throwExternal() {
        // El cliente elige su propia URL de documentación
        throw new ProblemException(
                "Error en la validación.",
                HttpStatus.BAD_REQUEST,
                "https://docs.saul.dev/errors/validation-guide"
        );
    }

    @GetMapping("/business-logic")
    public void throwBusiness() {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Regla de negocio incumplida.", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/annotated")
    public void throwAnnotated() {
        throw new CustomConflictException("Este recurso ya está siendo editado.");
    }

    @GetMapping("/not-found")
    public void throwResponseStatus() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso no encontrado.");
    }

    /**
     * Muestra la traducción automática si el mensaje coincide con una clave en messages.properties.
     * Si enviamos Accept-Language: es, se traducirá usando la clave 'error.business.default'.
     */
    @GetMapping("/i18n-key")
    public void throwI18nKey() {
        // 'error.business.default' está en messages.properties y messages_es.properties
        throw new BusinessException(ErrorCode.BAD_REQUEST, "error.business.default", HttpStatus.BAD_REQUEST);
    }

    /**
     * Muestra que si el texto NO es una clave, se devuelve tal cual (personalización total del cliente).
     */
    @GetMapping("/custom-message")
    public void throwCustomMessage() {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Este es un mensaje totalmente personalizado que no está en los properties.", HttpStatus.BAD_REQUEST);
    }

    /**
     * Muestra que el cliente puede definir sus propios mensajes en su PROPIO 'messages.properties'
     * (fuera del starter) y la librería los resolverá correctamente.
     */
    @GetMapping("/client-key")
    public void throwClientKey() {
        // 'sample.error.limit_reached' está definido en sample-project/src/main/resources/messages.properties
        throw new BusinessException(ErrorCode.FORBIDDEN, "sample.error.limit_reached", HttpStatus.FORBIDDEN);
    }
}
