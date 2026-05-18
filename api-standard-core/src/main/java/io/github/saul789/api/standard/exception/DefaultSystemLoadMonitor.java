package io.github.saul789.api.standard.exception;

import io.github.saul789.api.standard.ApiStandardProperties;
import java.lang.management.ManagementFactory;

/**
 * Default implementation of {@link SystemLoadMonitor} using JVM MXBeans.
 */
public class DefaultSystemLoadMonitor implements SystemLoadMonitor {

  private final ApiStandardProperties properties;
  private final com.sun.management.OperatingSystemMXBean osBean;

  public DefaultSystemLoadMonitor(ApiStandardProperties properties) {
    this.properties = properties;
    this.osBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  }

  @Override
  public boolean isHighLoad() {
    if (!properties.getDegradedMode().isEnabled()) {
      return false;
    }

    double cpuLoad = osBean.getCpuLoad(); // Returns a value between 0.0 and 1.0
    if (cpuLoad < 0) {
      // Returns -1 if not available
      return false;
    }

    double cpuPercentage = cpuLoad * 100.0;
    return cpuPercentage >= properties.getDegradedMode().getCpuThreshold();
  }
}
