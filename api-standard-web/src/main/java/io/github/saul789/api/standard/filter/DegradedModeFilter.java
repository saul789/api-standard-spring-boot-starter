package io.github.saul789.api.standard.filter;

import io.github.saul789.api.standard.ApiStandardProperties;
import io.github.saul789.api.standard.exception.SystemLoadMonitor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that adds the 'X-API-Degraded' header to the response when the system
 * is under high load.
 */
public class DegradedModeFilter extends OncePerRequestFilter {

  private final SystemLoadMonitor loadMonitor;
  private final ApiStandardProperties properties;

  public DegradedModeFilter(SystemLoadMonitor loadMonitor, ApiStandardProperties properties) {
    this.loadMonitor = loadMonitor;
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (properties.getDegradedMode().isEnabled() && loadMonitor.isHighLoad()
        && properties.getDegradedMode().isShowInHeader()) {
      response.setHeader("X-API-Degraded", "true");
    }

    filterChain.doFilter(request, response);
  }
}
