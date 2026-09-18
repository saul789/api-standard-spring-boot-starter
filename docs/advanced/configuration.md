# Configuración Avanzada ⚙️

Todas las propiedades soportadas en `application.yml`:

```yaml
api:
  standard:
    errors:
      # URL base para la documentación de errores (RFC 9457)
      type-base-uri: "https://developer.tuempresa.com/errors/"
      
    redaction:
      # Activa/desactiva el enmascaramiento de datos sensibles
      enabled: true
      # Expresión regular o lista de campos a censurar
      fields: "password, token, apiKey, secret, credit_card"
      
    degraded-mode:
      # Activa/desactiva la protección contra DDoS
      enabled: true
      # Umbral de CPU antes de rechazar peticiones (85%)
      cpu-threshold: 85
      
    metrics:
      # Activa la exportación a Micrometer/Prometheus
      enabled: true
```
