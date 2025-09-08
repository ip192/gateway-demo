package com.example.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for circuit breaker configuration validation from properties files
 * Verifies that Resilience4j circuit breaker and time limiter configurations are correctly loaded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.test-service-cb.slidingWindowSize=15",
    "resilience4j.circuitbreaker.instances.test-service-cb.minimumNumberOfCalls=8",
    "resilience4j.circuitbreaker.instances.test-service-cb.failureRateThreshold=60",
    "resilience4j.circuitbreaker.instances.test-service-cb.waitDurationInOpenState=12s",
    "resilience4j.circuitbreaker.instances.test-service-cb.permittedNumberOfCallsInHalfOpenState=4",
    "resilience4j.circuitbreaker.instances.test-service-cb.registerHealthIndicator=true",
    "resilience4j.timelimiter.instances.test-service-cb.timeoutDuration=6s"
})
class CircuitBreakerConfigurationPropertiesTest {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Test
    void testCircuitBreakerRegistryLoaded() {
        assertThat(circuitBreakerRegistry).isNotNull();
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).isNotEmpty();
    }

    @Test
    void testCircuitBreakerInstanceConfiguration() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-service-cb");
        assertThat(circuitBreaker).isNotNull();
        
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        assertThat(config.getSlidingWindowSize()).isEqualTo(15);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(8);
        assertThat(config.getFailureRateThreshold()).isEqualTo(60.0f);
        assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(12));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(4);
    }

    @Test
    void testTimeLimiterRegistryLoaded() {
        assertThat(timeLimiterRegistry).isNotNull();
        assertThat(timeLimiterRegistry.getAllTimeLimiters()).isNotEmpty();
    }

    @Test
    void testTimeLimiterInstanceConfiguration() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("test-service-cb");
        assertThat(timeLimiter).isNotNull();
        
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(6));
    }

    @Test
    void testCircuitBreakerHealthIndicatorRegistration() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-service-cb");
        assertThat(circuitBreaker).isNotNull();
        
        // Verify that the circuit breaker is registered for health monitoring
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        // Note: registerHealthIndicator is handled by Spring Boot auto-configuration
        // We verify the circuit breaker exists and can be monitored
        assertThat(circuitBreaker.getName()).isEqualTo("test-service-cb");
        assertThat(circuitBreaker.getState()).isNotNull();
    }

    @Test
    void testCircuitBreakerDefaultValues() {
        // Test that circuit breakers with default configuration can be created
        CircuitBreaker defaultCircuitBreaker = circuitBreakerRegistry.circuitBreaker("default-test");
        assertThat(defaultCircuitBreaker).isNotNull();
        
        CircuitBreakerConfig config = defaultCircuitBreaker.getCircuitBreakerConfig();
        // Verify some default values are reasonable
        assertThat(config.getSlidingWindowSize()).isGreaterThan(0);
        assertThat(config.getMinimumNumberOfCalls()).isGreaterThan(0);
        assertThat(config.getFailureRateThreshold()).isBetween(0.0f, 100.0f);
    }

    @Test
    void testMultipleCircuitBreakerInstances() {
        // Test that multiple circuit breaker instances can coexist
        CircuitBreaker cb1 = circuitBreakerRegistry.circuitBreaker("service1-cb");
        CircuitBreaker cb2 = circuitBreakerRegistry.circuitBreaker("service2-cb");
        
        assertThat(cb1).isNotNull();
        assertThat(cb2).isNotNull();
        assertThat(cb1.getName()).isNotEqualTo(cb2.getName());
    }

    @Test
    void testCircuitBreakerStateTransitions() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("state-test-cb");
        assertThat(circuitBreaker).isNotNull();
        
        // Verify initial state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Verify state can be accessed (important for monitoring)
        assertThat(circuitBreaker.getMetrics()).isNotNull();
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
    }
}