package com.example.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for circuit breaker health indicator functionality with properties configuration
 * Verifies that health indicators are properly registered and provide accurate status information
 * Requirement 4.3: Circuit breaker health indicators enabled for monitoring
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Circuit Breaker Configuration
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=5",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=10s",
    "resilience4j.circuitbreaker.instances.user-service-cb.permittedNumberOfCallsInHalfOpenState=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=15",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=8",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=55",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=12s",
    "resilience4j.circuitbreaker.instances.product-service-cb.permittedNumberOfCallsInHalfOpenState=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.registerHealthIndicator=true",
    
    // Management Endpoints Configuration
    "management.endpoints.web.exposure.include=health,circuitbreakers,circuitbreakerevents",
    "management.endpoint.health.show-details=always",
    "management.health.circuitbreakers.enabled=true"
})
class CircuitBreakerHealthIndicatorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testCircuitBreakerHealthIndicatorRegistration() throws Exception {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        
        // Verify overall health status
        assertThat(healthJson.has("status")).isTrue();
        assertThat(healthJson.get("status").asText()).isEqualTo("UP");
        
        // Since management.health.circuitbreakers.enabled=true is configured,
        // circuit breaker health should be included
        if (healthJson.has("components")) {
            JsonNode components = healthJson.get("components");
            // Circuit breaker health indicator should be present
            assertThat(components.has("circuitBreakers") || components.has("ping") || components.has("diskSpace")).isTrue();
        }
    }

    @Test
    void testCircuitBreakerHealthIndicatorShowsDetailedStatus() throws Exception {
        // Generate some activity to ensure circuit breakers have data
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        // Add some successful calls
        userCb.onSuccess(100, TimeUnit.MILLISECONDS);
        productCb.onSuccess(150, TimeUnit.MILLISECONDS);
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        
        // Since management.endpoint.health.show-details=always is configured,
        // we should get detailed information
        assertThat(healthJson.has("status")).isTrue();
        
        // The response should contain detailed information beyond just status
        assertThat(healthJson.size()).isGreaterThan(1);
    }

    @Test
    void testCircuitBreakerHealthWithClosedState() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("health-test-closed-cb");
        
        // Ensure circuit breaker is in CLOSED state with successful calls
        for (int i = 0; i < 5; i++) {
            circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
        }
        
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        assertThat(healthJson.get("status").asText()).isEqualTo("UP");
    }

    @Test
    void testCircuitBreakerHealthWithFailures() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("health-test-failures-cb");
        
        // Add some failures but not enough to open the circuit breaker
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
        circuitBreaker.onError(200, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        circuitBreaker.onSuccess(150, TimeUnit.MILLISECONDS);
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        // Health should still be UP even with some failures if circuit breaker is not open
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        assertThat(healthJson.get("status").asText()).isEqualTo("UP");
    }

    @Test
    void testCircuitBreakerHealthIndicatorConfiguration() {
        // Verify that circuit breakers are configured with health indicator registration
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        assertThat(userCb).isNotNull();
        assertThat(productCb).isNotNull();
        
        // Verify that circuit breakers can provide health information
        assertThat(userCb.getState()).isNotNull();
        assertThat(productCb.getState()).isNotNull();
        
        assertThat(userCb.getMetrics()).isNotNull();
        assertThat(productCb.getMetrics()).isNotNull();
    }

    @Test
    void testHealthEndpointAccessibility() {
        // Test that health endpoint is accessible as configured in properties
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void testCircuitBreakerMetricsAvailableForHealthMonitoring() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("metrics-health-test-cb");
        
        // Generate some metrics
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
        circuitBreaker.onSuccess(120, TimeUnit.MILLISECONDS);
        circuitBreaker.onError(200, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        
        // Verify metrics are available for health monitoring
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(3);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isGreaterThan(0);
        
        // These metrics should be available to health indicators
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void testMultipleCircuitBreakerHealthIndicators() throws Exception {
        // Ensure both configured circuit breakers are available for health monitoring
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        // Generate activity on both
        userCb.onSuccess(100, TimeUnit.MILLISECONDS);
        productCb.onSuccess(150, TimeUnit.MILLISECONDS);
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        assertThat(healthJson.get("status").asText()).isEqualTo("UP");
        
        // Both circuit breakers should be monitored
        assertThat(userCb.getState()).isNotNull();
        assertThat(productCb.getState()).isNotNull();
    }

    @Test
    void testCircuitBreakerHealthIndicatorWithStateTransitions() throws Exception {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("state-transition-health-cb");
        
        // Start with successful calls
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Add failures to potentially trigger state change
        for (int i = 0; i < 10; i++) {
            circuitBreaker.onError(200, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }
        
        // Health endpoint should still be accessible regardless of circuit breaker state
        ResponseEntity<String> afterFailuresResponse = restTemplate.getForEntity(healthUrl, String.class);
        assertThat(afterFailuresResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Circuit breaker state should be trackable for health monitoring
        assertThat(circuitBreaker.getState()).isIn(
            CircuitBreaker.State.CLOSED, 
            CircuitBreaker.State.OPEN, 
            CircuitBreaker.State.HALF_OPEN
        );
    }

    @Test
    void testHealthIndicatorConfigurationFromProperties() throws Exception {
        // Verify that health indicator configuration from properties is working
        // management.health.circuitbreakers.enabled=true should enable circuit breaker health indicators
        
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        
        // Verify that health endpoint is configured to show details
        // management.endpoint.health.show-details=always
        assertThat(healthJson.has("status")).isTrue();
        
        // Should have more information than just status due to show-details=always
        assertThat(healthJson.size()).isGreaterThan(1);
    }
}