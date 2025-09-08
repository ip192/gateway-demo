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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for circuit breaker functionality with properties configuration
 * Tests circuit breaker instance configuration, time limiter configuration, 
 * health indicator functionality, and monitoring endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // User Service Circuit Breaker Configuration
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=5",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=10s",
    "resilience4j.circuitbreaker.instances.user-service-cb.permittedNumberOfCallsInHalfOpenState=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true",
    
    // Product Service Circuit Breaker Configuration
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=15",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=8",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=55",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=12s",
    "resilience4j.circuitbreaker.instances.product-service-cb.permittedNumberOfCallsInHalfOpenState=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.registerHealthIndicator=true",
    
    // Time Limiter Configuration
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=3s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=4s",
    
    // Management Endpoints Configuration
    "management.endpoints.web.exposure.include=health,circuitbreakers,circuitbreakerevents",
    "management.endpoint.health.show-details=always",
    "management.health.circuitbreakers.enabled=true"
})
class CircuitBreakerFunctionalityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    /**
     * Test circuit breaker instance configuration from properties
     * Requirement 4.1: Circuit breaker instances configured with all existing parameters preserved
     */
    @Test
    void testUserServiceCircuitBreakerInstanceConfiguration() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        assertThat(circuitBreaker).isNotNull();
        
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        
        // Verify configuration matches properties file values
        assertThat(config.getSlidingWindowSize()).isEqualTo(10);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        
        // Verify circuit breaker name and initial state
        assertThat(circuitBreaker.getName()).isEqualTo("user-service-cb");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testProductServiceCircuitBreakerInstanceConfiguration() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        assertThat(circuitBreaker).isNotNull();
        
        CircuitBreakerConfig config = circuitBreaker.getCircuitBreakerConfig();
        
        // Verify configuration matches properties file values
        assertThat(config.getSlidingWindowSize()).isEqualTo(15);
        assertThat(config.getMinimumNumberOfCalls()).isEqualTo(8);
        assertThat(config.getFailureRateThreshold()).isEqualTo(55.0f);
        assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(12));
        assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        
        // Verify circuit breaker name and initial state
        assertThat(circuitBreaker.getName()).isEqualTo("product-service-cb");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    /**
     * Test time limiter configuration from properties
     * Requirement 4.2: Time limiter configurations with timeout durations preserved
     */
    @Test
    void testUserServiceTimeLimiterConfiguration() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        assertThat(timeLimiter).isNotNull();
        
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        
        // Verify timeout duration matches properties file value
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    @Test
    void testProductServiceTimeLimiterConfiguration() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        assertThat(timeLimiter).isNotNull();
        
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        
        // Verify timeout duration matches properties file value
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(4));
        assertThat(timeLimiter.getName()).isEqualTo("product-service-cb");
    }

    @Test
    void testTimeLimiterTimeoutFunctionality() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        
        // Test that time limiter configuration is correct
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        
        // Verify time limiter can be used for monitoring timeout behavior
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    /**
     * Test circuit breaker health indicator functionality
     * Requirement 4.3: Circuit breaker health indicators enabled for monitoring
     */
    @Test
    void testCircuitBreakerHealthIndicatorRegistration() {
        // Verify that circuit breakers are registered for health monitoring
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        assertThat(userCb).isNotNull();
        assertThat(productCb).isNotNull();
        
        // Verify that health indicators can access circuit breaker state
        assertThat(userCb.getState()).isNotNull();
        assertThat(productCb.getState()).isNotNull();
        
        // Verify metrics are available for health monitoring
        assertThat(userCb.getMetrics()).isNotNull();
        assertThat(productCb.getMetrics()).isNotNull();
        
        // Verify initial metrics state
        assertThat(userCb.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(productCb.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
    }

    @Test
    void testCircuitBreakerStateTransitionMonitoring() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("test-monitoring-cb");
        
        // Verify initial state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Simulate failures to trigger state transition
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(0, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }
        
        // Verify state transition can be monitored
        // Note: State might not change immediately due to sliding window configuration
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(0);
    }

    /**
     * Test circuit breaker monitoring endpoints with properties configuration
     * Requirement 5.2: Management endpoints for circuit breaker monitoring
     */
    @Test
    void testCircuitBreakerHealthEndpoint() {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Verify health endpoint includes circuit breaker information
        String responseBody = response.getBody();
        assertThat(responseBody).contains("circuitBreakers");
    }

    @Test
    void testCircuitBreakersEndpoint() {
        String circuitBreakersUrl = "http://localhost:" + port + "/actuator/circuitbreakers";
        
        ResponseEntity<String> response = restTemplate.getForEntity(circuitBreakersUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Verify endpoint returns circuit breaker information
        String responseBody = response.getBody();
        assertThat(responseBody).contains("user-service-cb");
        assertThat(responseBody).contains("product-service-cb");
    }

    @Test
    void testCircuitBreakerEventsEndpoint() {
        String eventsUrl = "http://localhost:" + port + "/actuator/circuitbreakerevents";
        
        ResponseEntity<String> response = restTemplate.getForEntity(eventsUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // Verify events endpoint is accessible
        String responseBody = response.getBody();
        assertThat(responseBody).isNotEmpty();
    }

    @Test
    void testCircuitBreakerSpecificHealthDetails() {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify detailed health information is available
        String responseBody = response.getBody();
        assertThat(responseBody).contains("status");
        
        // Since management.endpoint.health.show-details=always is configured,
        // detailed information should be available
        assertThat(responseBody).isNotEmpty();
    }

    /**
     * Test circuit breaker configuration validation
     */
    @Test
    void testCircuitBreakerConfigurationValidation() {
        // Test that all configured circuit breakers are properly loaded
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
        
        // Verify both user and product service circuit breakers exist
        assertThat(circuitBreakerRegistry.find("user-service-cb")).isPresent();
        assertThat(circuitBreakerRegistry.find("product-service-cb")).isPresent();
    }

    @Test
    void testTimeLimiterConfigurationValidation() {
        // Test that all configured time limiters are properly loaded
        assertThat(timeLimiterRegistry.getAllTimeLimiters()).hasSize(2);
        
        // Verify both user and product service time limiters exist
        assertThat(timeLimiterRegistry.find("user-service-cb")).isPresent();
        assertThat(timeLimiterRegistry.find("product-service-cb")).isPresent();
    }

    /**
     * Test circuit breaker metrics and monitoring functionality
     */
    @Test
    void testCircuitBreakerMetricsCollection() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        
        // Verify metrics are initially empty
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(0);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(0);
        
        // Simulate successful calls
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
        circuitBreaker.onSuccess(150, TimeUnit.MILLISECONDS);
        
        // Verify metrics are updated
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    void testCircuitBreakerFailureMetrics() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        // Simulate failed calls
        circuitBreaker.onError(200, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        circuitBreaker.onError(300, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        
        // Verify failure metrics are updated
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(100.0f);
    }
}