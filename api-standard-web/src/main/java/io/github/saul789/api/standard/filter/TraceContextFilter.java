package io.github.saul789.api.standard.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that establishes a W3C Trace Context ({@code traceparent}) for
 * every incoming
 * request.
 *
 * <p>
 * If the request already carries a valid {@code traceparent} header the
 * existing trace-id is
 * reused; otherwise a new one is generated. The resolved trace-id is stored in
 * {@link MDC} under
 * the key {@code "traceId"} so it is available to all loggers and to exception
 * handlers that embed
 * it in RFC 9457 responses.
 *
 * <p>
 * The MDC context is <em>always</em> cleared in a {@code finally} block to
 * prevent trace-id
 * leakage across requests on reused threads.
 *
 * @see <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 */
public class TraceContextFilter extends OncePerRequestFilter {

  private static final String TRACEPARENT = "traceparent";
  private static final String MDC_KEY = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = resolveTraceId(request);

    MDC.put(MDC_KEY, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  /**
   * Resolves the trace-id by checking W3C traceparent headers, pre-existing MDC keys
   * populated by APM agents (Datadog, OpenTelemetry, New Relic, Dynatrace), and falling
   * back to generating a fresh UUID.
   */
  private String resolveTraceId(HttpServletRequest request) {
    // 1. Priority: Check if the standard W3C traceparent header is present in the request
    String traceparentHeader = request.getHeader(TRACEPARENT);
    if (isValidTraceparent(traceparentHeader)) {
      return traceparentHeader.split("-")[1];
    }

    // 2. Priority: Check if a container APM agent already injected the trace ID into MDC
    // OpenTelemetry / Micrometer standard key
    String agentTraceId = MDC.get("traceId");
    if (agentTraceId == null || agentTraceId.trim().isEmpty()) {
      agentTraceId = MDC.get("trace_id");
    }
    // Datadog agent key
    if (agentTraceId == null || agentTraceId.trim().isEmpty()) {
      agentTraceId = MDC.get("dd.trace_id");
    }
    // New Relic agent key
    if (agentTraceId == null || agentTraceId.trim().isEmpty()) {
      agentTraceId = MDC.get("trace.id");
    }
    // Dynatrace agent key
    if (agentTraceId == null || agentTraceId.trim().isEmpty()) {
      agentTraceId = MDC.get("dt.trace_id");
    }

    if (agentTraceId != null && !agentTraceId.trim().isEmpty()) {
      return agentTraceId;
    }

    // 3. Priority: Absolute fallback - generate a new unique trace ID
    return generateTraceId();
  }

  /**
   * Generates a new W3C-compliant trace-id as a 32-character lowercase hex
   * string.
   *
   * @return a new unique trace-id
   */
  private String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Validates a {@code traceparent} header value against the W3C specification.
   *
   * <p>
   * A valid value must have exactly four dash-separated parts where the second
   * part (trace-id)
   * is 32 lowercase hex characters and the third part (parent-id) is 16 lowercase
   * hex characters.
   *
   * @param traceparent the raw header value; may be {@code null}
   * @return {@code true} if the value conforms to the W3C format
   */
  private boolean isValidTraceparent(String traceparent) {
    if (traceparent == null) {
      return false;
    }
    String[] parts = traceparent.split("-");
    if (parts.length != 4) {
      return false;
    }
    return parts[1].matches("[0-9a-f]{32}") && parts[2].matches("[0-9a-f]{16}");
  }
}
