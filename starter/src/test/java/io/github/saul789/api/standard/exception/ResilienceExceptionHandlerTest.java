package io.github.saul789.api.standard.exception;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.saul789.api.standard.ApiStandardProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResilienceExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);

        ProblemDetailService problemDetailService = new ProblemDetailService(
                messageSource,
                new ApiStandardProperties(),
                new ArrayList<>());

        ResilienceExceptionHandler handler = new ResilienceExceptionHandler(problemDetailService);

        this.mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void shouldHandleCallNotPermittedException() throws Exception {
        mockMvc.perform(get("/test/circuit-breaker"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("urn:problem-type:service-unavailable"))
                .andExpect(jsonPath("$.detail").value("error.circuit_breaker_open"))
                .andExpect(jsonPath("$.title").value("Service Unavailable"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void shouldHandleRequestNotPermitted() throws Exception {
        mockMvc.perform(get("/test/rate-limiter"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("urn:problem-type:too-many-requests"))
                .andExpect(jsonPath("$.detail").value("error.too_many_requests"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void shouldHandleBulkheadFullException() throws Exception {
        mockMvc.perform(get("/test/bulkhead"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.type").value("urn:problem-type:too-many-requests"))
                .andExpect(jsonPath("$.detail").value("error.too_many_requests"))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @RestController
    static class DummyController {
        @GetMapping("/test/circuit-breaker")
        public void circuitBreaker() {
            throw CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("test"));
        }

        @GetMapping("/test/rate-limiter")
        public void rateLimiter() {
            RateLimiter limit = RateLimiter.of("test", RateLimiterConfig.custom()
                    .limitForPeriod(1).limitRefreshPeriod(Duration.ofSeconds(1)).build());
            throw RequestNotPermitted.createRequestNotPermitted(limit);
        }

        @GetMapping("/test/bulkhead")
        public void bulkhead() {
            Bulkhead bulk = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).build());
            throw BulkheadFullException.createBulkheadFullException(bulk);
        }
    }
}
