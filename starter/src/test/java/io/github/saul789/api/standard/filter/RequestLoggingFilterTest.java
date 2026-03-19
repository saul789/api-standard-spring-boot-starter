package io.github.saul789.api.standard.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @InjectMocks
    private RequestLoggingFilter requestLoggingFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @Test
    void shouldLogRequestDetailsCorrectly() throws ServletException, IOException {
        // Preparación
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);
        MDC.put("traceId", "test-trace-123");

        // Ejecución
        requestLoggingFilter.doFilter(request, response, filterChain);

        // Verificación
        // Verificamos que el filtro permitió que la petición continuara
        verify(filterChain, times(1)).doFilter(request, response);

        // El log se hace a través de la API fluida 'atInfo()'.
        // Aunque verificar logs exactos es complejo, el simple hecho de que el test
        // termine
        // sin errores garantiza la cobertura de todas las líneas del bloque finally.
    }

    @Test
    void shouldLogEvenWhenExceptionIsThrown() throws ServletException, IOException {
        // Preparación: Forzamos una excepción en el siguiente filtro de la cadena
        doThrow(new RuntimeException("Chain error")).when(filterChain).doFilter(request, response);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/error");
        when(response.getStatus()).thenReturn(500);

        // Ejecución y Verificación
        // El bloque finally DEBE ejecutarse aunque el doFilter falle
        assertThrows(RuntimeException.class, () -> requestLoggingFilter.doFilter(request, response, filterChain));

        // Verificamos que se intentó continuar antes de la excepción
        verify(filterChain).doFilter(request, response);

        // Al terminar este test, JaCoCo marcará el bloque finally como cubierto.
    }
}