package io.github.saul789.api.standard.exception;

/**
 * Monitor for system load (CPU, Memory).
 */
public interface SystemLoadMonitor {

  /**
   * Checks if the system is under high load and should run in degraded mode.
   *
   * @return true if the system is under high load, false otherwise.
   */
  boolean isHighLoad();
}
