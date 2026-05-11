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
 * Servlet filter that establishes a W3C Trace Context ({@code traceparent}) for every incoming
 * request.
 *
 * <p>If the request already carries a valid {@code traceparent} header the existing trace-id is
 * reused; otherwise a new one is generated. The resolved trace-id is stored in {@link MDC} under
 * the key {@code "traceId"} so it is available to all loggers and to exception handlers that embed
 * it in RFC 9457 responses.
 *
 * <p>The MDC context is <em>always</em> cleared in a {@code finally} block to prevent trace-id
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

    String traceparentHeader = request.getHeader(TRACEPARENT);

    String traceId =
        isValidTraceparent(traceparentHeader) ? traceparentHeader.split("-")[1] : generateTraceId();

    MDC.put(MDC_KEY, traceId);
    response.setHeader(TRACEPARENT, buildTraceparent(traceId));

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  /**
   * Generates a new W3C-compliant trace-id as a 32-character lowercase hex string.
   *
   * @return a new unique trace-id
   */
  private String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  /**
   * Builds a minimal {@code traceparent} header value from the given trace-id.
   *
   * <p>Uses version {@code 00}, a newly generated 16-character span-id, and sampled flag {@code
   * 01}.
   *
   * @param traceId a 32-character lowercase hex trace-id
   * @return a well-formed {@code traceparent} string
   */
  private String buildTraceparent(String traceId) {
    String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    return "00-" + traceId + "-" + spanId + "-01";
  }

  /**
   * Validates a {@code traceparent} header value against the W3C specification.
   *
   * <p>A valid value must have exactly four dash-separated parts where the second part (trace-id)
   * is 32 lowercase hex characters and the third part (parent-id) is 16 lowercase hex characters.
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
