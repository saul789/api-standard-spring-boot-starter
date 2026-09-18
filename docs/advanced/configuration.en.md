# Advanced Configuration ⚙️

All supported properties in `application.yml`:

```yaml
api:
  standard:
    errors:
      # Base URL for error documentation (RFC 9457)
      type-base-uri: "https://developer.yourcompany.com/errors/"
      
    redaction:
      # Enable/disable sensitive data masking
      enabled: true
      # Regular expression or list of fields to censor
      fields: "password, token, apiKey, secret, credit_card"
      
    degraded-mode:
      # Enable/disable DDoS protection
      enabled: true
      # CPU threshold before rejecting requests (85%)
      cpu-threshold: 85
      
    metrics:
      # Enable Micrometer/Prometheus export
      enabled: true
```
