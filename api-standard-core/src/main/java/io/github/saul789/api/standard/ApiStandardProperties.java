package io.github.saul789.api.standard;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the API standard starter. */
@ConfigurationProperties(prefix = "api.standard")
public class ApiStandardProperties {

  private final Errors errors = new Errors();
  private final DegradedMode degradedMode = new DegradedMode();

  public Errors getErrors() {
    return errors;
  }

  public DegradedMode getDegradedMode() {
    return degradedMode;
  }

  public static class Errors {
    /**
     * Base URI for the 'type' field in the ProblemDetail responses. Default is "urn:problem-type:".
     */
    private String typeBaseUri = "urn:problem-type:";

    private Map<String, String> typeOverrides = new HashMap<>();

    public String getTypeBaseUri() {
      return typeBaseUri;
    }

    public void setTypeBaseUri(String typeBaseUri) {
      this.typeBaseUri = typeBaseUri;
    }

    public Map<String, String> getTypeOverrides() {
      return Collections.unmodifiableMap(typeOverrides);
    }

    public void setTypeOverrides(Map<String, String> typeOverrides) {
      this.typeOverrides = typeOverrides != null ? new HashMap<>(typeOverrides) : new HashMap<>();
    }
  }

  public static class DegradedMode {
    /** Whether to enable degraded mode. */
    private boolean enabled = true;

    /** CPU usage threshold percentage to trigger degraded mode. */
    private double cpuThreshold = 80.0;

    /** Whether to show the 'degraded' flag in the JSON response. */
    private boolean showInJson = false;

    /** Whether to show the 'X-API-Degraded' header in the response. */
    private boolean showInHeader = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public double getCpuThreshold() {
      return cpuThreshold;
    }

    public void setCpuThreshold(double cpuThreshold) {
      this.cpuThreshold = cpuThreshold;
    }

    public boolean isShowInJson() {
      return showInJson;
    }

    public void setShowInJson(boolean showInJson) {
      this.showInJson = showInJson;
    }

    public boolean isShowInHeader() {
      return showInHeader;
    }

    public void setShowInHeader(boolean showInHeader) {
      this.showInHeader = showInHeader;
    }
  }
}
