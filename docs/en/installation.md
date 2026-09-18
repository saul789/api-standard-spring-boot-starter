# Installation & Setup

This guide covers how to install the `api-standard-spring-boot-starter` and the initial configuration options available.

---

## Minimum Requirements

The library has been verified with the following stack:

| Requirement | Version | Notes |
| :--- | :--- | :--- |
| **Java** | 21 | Uses features like Pattern Matching and Records. |
| **Spring Boot** | 4.0.6 | Verified with this specific version. |
| **Spring Web** | Included | Designed for Spring MVC applications. |

---

## Compatibility Matrix

> [!WARNING]
> **Limited Verification**: The current project codebase and documentation explicitly reference **Spring Boot 4.0.6** and **Java 21**. Compatibility with older versions (such as Spring Boot 3.x or Java 17) or other frameworks has not been verified or documented in the repository.

---

## Installation

Add the dependency to your build system. Since this is a Spring Boot starter, it will auto-configure itself once present on the classpath.

=== "Maven"

    Add the following to your `pom.xml`:

    ```xml
    <dependency>
        <groupId>io.github.saul789</groupId>
        <artifactId>api-standard-spring-boot-starter</artifactId>
        <version>2.0.0</version>
    </dependency>
    ```

=== "Gradle"

    Add the following to your `build.gradle`:

    ```groovy
    implementation 'io.github.saul789:api-standard-spring-boot-starter:2.0.0'
    ```

---

## Initial Configuration

You can customize the behavior of the starter by adding the following properties to your `application.yml` or `application.properties`.

Here are all the properties available in the source code (`ApiStandardProperties.java`):

```yaml
api:
  standard:
    errors:
      # Base URI for the 'type' field in ProblemDetail responses.
      type-base-uri: "urn:problem-type:"
      
      # Map of error codes to custom documentation URLs.
      type-overrides:
        # Example:
        # NOT_FOUND: "https://docs.example.com/errors/not-found"
        
    degraded-mode:
      # Whether to enable the degraded mode filter and enricher.
      enabled: true
      
      # CPU usage threshold percentage to trigger degraded mode.
      cpu-threshold: 80.0
      
      # Whether to show the 'degraded' flag in the JSON response.
      show-in-json: false
      
      # Whether to show the 'X-API-Degraded' header in the response.
      show-in-header: true
      
    redaction:
      # Whether to enable the PII redaction engine.
      enabled: true
      
      # The mask to replace sensitive field values with.
      mask: "[REDACTED]"
      
      # List of field names whose values should be redacted.
      fields:
        - "password"
        - "token"
        - "secret"
        - "creditCard"
        - "cvv"
        - "ssn"
        - "authorization"
```

---

## JS Ecosystem & Platform Compatibility (SSR/ESM/CJS)

> [!NOTE]
> **Not Applicable**: This is a Java/Spring Boot server-side library. JavaScript module systems (ESM, CJS) and Server-Side Rendering (SSR) paradigms do not apply. 
> 
> **WebFlux Support**: Support for Spring WebFlux is not explicitly implemented or tested in the current codebase (the current handlers are based on Spring MVC and `HttpServletRequest`).

---

## Common Issues & Troubleshooting

### Issue: Circular Dependency
*   **Symptom**: Spring fails to start with a circular reference error involving `GlobalExceptionHandler` or security filters.
*   **Root Cause**: Custom security configurations injecting the exception handler too early.
*   **Solution**: Ensure your security configuration does not eagerly depend on the error handling beans or adjust the `@Order` of your filters.

### Issue: CPU Load Not Available
*   **Symptom**: Degraded mode is enabled but never triggers or logs warnings.
*   **Root Cause**: The underlying operating system or JVM does not support `OperatingSystemMXBean.getCpuLoad()` (returns -1).
*   **Solution**: In these environments, the library falls back to assuming normal load. Custom implementation of `SystemLoadMonitor` is required if you need to use another metric.
