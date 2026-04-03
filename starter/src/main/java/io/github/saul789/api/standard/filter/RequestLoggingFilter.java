package io.github.saul789.api.standard.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that logs a single structured line per HTTP request.
 *
 * <p>The log entry is emitted <em>after</em> the response is committed
 * (in the {@code finally} block) so it always includes the final HTTP status
 * and the accurate elapsed time, even when a downstream filter or handler
 * throws an exception.
 *
 * <p>The trace-id is read from the {@code traceparent} response header
 * (set by {@link TraceContextFilter}) rather than from MDC directly, so
 * this filter can run in any position in the filter chain.
 */
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String traceparent = response.getHeader("traceparent");
            String traceId = traceparent != null ? traceparent.split("-")[1] : "unknown";

            log.atInfo()
                    .setMessage("Request method={} path={} status={} durationMs={} traceId={}")
                    .addArgument(request.getMethod())
                    .addArgument(request.getRequestURI())
                    .addArgument(response.getStatus())
                    .addArgument(duration)
                    .addArgument(traceId)
                    .log();
        }
    }
}