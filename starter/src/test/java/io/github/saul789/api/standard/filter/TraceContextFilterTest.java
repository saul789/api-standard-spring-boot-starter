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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceContextFilterTest {

    @InjectMocks
    private TraceContextFilter traceContextFilter;

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
    void shouldReuseTraceIdFromExistingTraceparent() throws ServletException, IOException {
        // Preparación: Un traceparent válido según W3C (4 partes separadas por '-')
        // 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01
        String existingTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String traceparent = "00-" + existingTraceId + "-00f067aa0ba902b7-01";

        when(request.getHeader("traceparent")).thenReturn(traceparent);

        // Ejecución
        // Usamos una implementación real de doFilter para verificar que el MDC esté
        // puesto DURANTE la ejecución
        doAnswer(invocation -> {
            assertEquals(existingTraceId, MDC.get("traceId"));
            return null;
        }).when(filterChain).doFilter(request, response);

        traceContextFilter.doFilter(request, response, filterChain);

        // Verificación
        verify(response).setHeader("traceparent", traceparent);
        assertNull(MDC.get("traceId"), "El MDC debería limpiarse en el bloque finally");
    }

    @Test
    void shouldGenerateNewTraceIdWhenHeaderIsMissing() throws ServletException, IOException {
        // Preparación: Header nulo
        when(request.getHeader("traceparent")).thenReturn(null);

        // Ejecución
        traceContextFilter.doFilter(request, response, filterChain);

        // Verificación
        // Verificamos que se haya generado y seteado un header en la respuesta
        verify(response).setHeader(eq("traceparent"), contains("-"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGenerateNewTraceIdWhenHeaderIsInvalid() throws ServletException, IOException {
        // Preparación: Header con formato incorrecto (menos de 4 partes)
        when(request.getHeader("traceparent")).thenReturn("00-invalid-format");

        // Ejecución
        traceContextFilter.doFilter(request, response, filterChain);

        // Verificación: Debe tratarlo como si no existiera y generar uno nuevo
        verify(response).setHeader(eq("traceparent"), anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldClearMDCEvenOnException() throws ServletException, IOException {
        // Preparación
        when(request.getHeader("traceparent")).thenReturn(null);
        doThrow(new RuntimeException("Filter error")).when(filterChain).doFilter(request, response);

        // Ejecución y Verificación
        assertThrows(RuntimeException.class, () -> traceContextFilter.doFilter(request, response, filterChain));

        // El finally debe haber limpiado el MDC
        assertNull(MDC.get("traceId"));
    }
}