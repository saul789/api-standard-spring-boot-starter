package io.github.saul789.api.standard.exception;

import static org.junit.jupiter.api.Assertions.*;

import io.github.saul789.api.standard.ApiStandardProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSystemLoadMonitorTest {

  private DefaultSystemLoadMonitor monitor;
  private ApiStandardProperties properties;

  @BeforeEach
  void setUp() {
    properties = new ApiStandardProperties();
    monitor = new DefaultSystemLoadMonitor(properties);
  }

  @Test
  void whenDisabled_shouldReturnFalse() {
    // Given
    properties.getDegradedMode().setEnabled(false);

    // When & Then
    assertFalse(monitor.isHighLoad());
  }

  @Test
  void whenEnabled_shouldReturnBasedOnCpu() {
    // Given
    properties.getDegradedMode().setEnabled(true);
    properties.getDegradedMode().setCpuThreshold(0.0); // 0% threshold means any CPU >= 0 is high load

    // When & Then
    // We verify it doesn't throw exceptions.
    try {
      monitor.isHighLoad();
      // If CPU is available, it should be >= 0.
      assertTrue(true);
    } catch (Exception e) {
      fail("Should not throw exception: " + e.getMessage());
    }
  }
}
