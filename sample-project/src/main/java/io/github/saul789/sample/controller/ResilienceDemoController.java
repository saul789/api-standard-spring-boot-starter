package io.github.saul789.sample.controller;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/demo/resilience")
public class ResilienceDemoController {

    @GetMapping("/circuit-breaker")
    public void circuitBreaker() {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("demo-circuit");
        throw CallNotPermittedException.createCallNotPermittedException(cb);
    }

    @GetMapping("/rate-limiter")
    public void rateLimiter() {
        RateLimiter limit = RateLimiter.of("demo-rate", RateLimiterConfig.custom()
                .limitForPeriod(1).limitRefreshPeriod(Duration.ofSeconds(1)).build());
        throw RequestNotPermitted.createRequestNotPermitted(limit);
    }

    @GetMapping("/bulkhead")
    public void bulkhead() {
        Bulkhead limit = Bulkhead.of("demo-bulkhead", BulkheadConfig.custom().maxConcurrentCalls(1).build());
        throw BulkheadFullException.createBulkheadFullException(limit);
    }

    @GetMapping("/timeout")
    public void timeout() throws TimeoutException {
        throw new TimeoutException("The upstream service did not respond in time");
    }
}
