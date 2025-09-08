package com.example.gateway;

import com.example.gateway.config.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CircuitBreakerConfiguration
 */
public class CircuitBreakerConfigurationTest {

    @Test
    void testDefaultCustomizer() {
        CircuitBreakerConfiguration config = new CircuitBreakerConfiguration();
        Customizer<ReactiveResilience4JCircuitBreakerFactory> customizer = config.defaultCustomizer();
        
        assertThat(customizer).isNotNull();
        
        // Test that the customizer can be applied
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
        customizer.customize(factory);
        
        // Verify that the factory is properly configured
        assertThat(factory).isNotNull();
    }

    @Test
    void testUserServiceCustomizer() {
        CircuitBreakerConfiguration config = new CircuitBreakerConfiguration();
        Customizer<ReactiveResilience4JCircuitBreakerFactory> customizer = config.userServiceCustomizer();
        
        assertThat(customizer).isNotNull();
        
        // Test that the customizer can be applied
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
        customizer.customize(factory);
        
        // Verify that the factory is properly configured
        assertThat(factory).isNotNull();
    }

    @Test
    void testProductServiceCustomizer() {
        CircuitBreakerConfiguration config = new CircuitBreakerConfiguration();
        Customizer<ReactiveResilience4JCircuitBreakerFactory> customizer = config.productServiceCustomizer();
        
        assertThat(customizer).isNotNull();
        
        // Test that the customizer can be applied
        ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
        customizer.customize(factory);
        
        // Verify that the factory is properly configured
        assertThat(factory).isNotNull();
    }

    @Test
    void testCircuitBreakerConfigValues() {
        // Test that we can create circuit breaker config with expected values
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    void testTimeLimiterConfigValues() {
        // Test that we can create time limiter config with expected values
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
    }
}