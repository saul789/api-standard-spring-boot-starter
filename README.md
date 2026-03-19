# Spring Boot Starter API Standard

Spring Boot starter to standardize API responses, error handling (RFC 7807), and structured logging with traceId support.

## Features

- **Standardized API Responses**: Consistent response format for all endpoints.
- **RFC 7807 Error Handling**: Standardized error responses for better client integration.
- **Structured Logging**: Structured logging with traceId support for better observability.
- **Trace Context Propagation**: Automatic traceId generation and propagation across requests.

## Usage

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.saul789</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Configuration

No configuration is required. The starter will automatically configure the following:

- `TraceContextFilter`: TraceId generation and propagation.
- `RequestLoggingFilter`: Request and response logging.
- `GlobalExceptionHandler`: Global exception handling.
- `ApiResponseAdvice`: Standardized API response wrapping.

## Example

```java
package com.example.demo;

import org.springframework.web.bind.annotation.*;

@RestController
public class DemoController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
