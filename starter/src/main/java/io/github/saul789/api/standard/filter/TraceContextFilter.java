package io.github.saul789.api.standard.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * W3C Trace Context filter (traceparent).
 */
public class TraceContextFilter extends OncePerRequestFilter {

    private static final String TRACEPARENT = "traceparent";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceparent = request.getHeader(TRACEPARENT);

        String traceId;

        if (traceparent != null && traceparent.split("-").length >= 4) {
            // formato: version-traceId-spanId-flags
            traceId = traceparent.split("-")[1];
        } else {
            traceId = generateTraceId();
            traceparent = buildTraceparent(traceId);
        }

        MDC.put(MDC_KEY, traceId);
        response.setHeader(TRACEPARENT, traceparent);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildTraceparent(String traceId) {
        String version = "00";
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String flags = "01";

        return version + "-" + traceId + "-" + spanId + "-" + flags;
    }
}