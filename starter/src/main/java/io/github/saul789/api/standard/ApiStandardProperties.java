package io.github.saul789.api.standard;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for the API standard starter.
 */
@ConfigurationProperties(prefix = "api.standard")
public class ApiStandardProperties {

    private final Errors errors = new Errors();

    public Errors getErrors() {
        return errors;
    }

    public static class Errors {
        /**
         * Base URI for the 'type' field in the ProblemDetail responses.
         * Default is "urn:problem-type:".
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
            return typeOverrides != null ? java.util.Collections.unmodifiableMap(typeOverrides) : null;
        }

        public void setTypeOverrides(Map<String, String> typeOverrides) {
            this.typeOverrides = typeOverrides != null ? new HashMap<>(typeOverrides) : new HashMap<>();
        }
    }
}
