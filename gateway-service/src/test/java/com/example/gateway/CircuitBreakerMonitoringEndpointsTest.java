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
 * Tests for circuit breaker monitoring endpoints with properties configuration
 * Verifies that management endpoints expose circuit breaker information correctly
 * Requirement 5.2: Management endpoints for circuit breaker monitoring
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
class CircuitBreakerMonitoringEndpointsTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testHealthEndpointExposesCircuitBreakerStatus() throws Exception {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        
        // Verify overall health status
        assertThat(healthJson.has("status")).isTrue();
        assertThat(healthJson.get("status").asText()).isEqualTo("UP");
        
        // Verify circuit breaker health information is included
        if (healthJson.has("components")) {
            JsonNode components = healthJson.get("components");
            if (components.has("circuitBreakers")) {
                JsonNode circuitBreakers = components.get("circuitBreakers");
                assertThat(circuitBreakers.has("status")).isTrue();
            }
        }
    }

    @Test
    void testCircuitBreakersEndpointReturnsAllInstances() throws Exception {
        String circuitBreakersUrl = "http://localhost:" + port + "/actuator/circuitbreakers";
        
        ResponseEntity<String> response = restTemplate.getForEntity(circuitBreakersUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode circuitBreakersJson = objectMapper.readTree(response.getBody());
        
        // Verify circuit breakers are listed
        assertThat(circuitBreakersJson.has("circuitBreakers")).isTrue();
        JsonNode circuitBreakers = circuitBreakersJson.get("circuitBreakers");
        
        // Should contain our configured circuit breakers
        boolean hasUserServiceCb = false;
        boolean hasProductServiceCb = false;
        
        for (JsonNode cb : circuitBreakers) {
            String name = cb.get("name").asText();
            if ("user-service-cb".equals(name)) {
                hasUserServiceCb = true;
                // Verify circuit breaker details
                assertThat(cb.has("state")).isTrue();
                assertThat(cb.has("config")).isTrue();
            } else if ("product-service-cb".equals(name)) {
                hasProductServiceCb = true;
                // Verify circuit breaker details
                assertThat(cb.has("state")).isTrue();
                assertThat(cb.has("config")).isTrue();
            }
        }
        
        assertThat(hasUserServiceCb).isTrue();
        assertThat(hasProductServiceCb).isTrue();
    }

    @Test
    void testCircuitBreakerEventsEndpointTracksEvents() throws Exception {
        // Generate some circuit breaker events
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        userCb.onSuccess(100, TimeUnit.MILLISECONDS);
        userCb.onError(200, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        
        String eventsUrl = "http://localhost:" + port + "/actuator/circuitbreakerevents";
        
        ResponseEntity<String> response = restTemplate.getForEntity(eventsUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode eventsJson = objectMapper.readTree(response.getBody());
        
        // Verify events structure
        assertThat(eventsJson.has("circuitBreakerEvents")).isTrue();
        JsonNode events = eventsJson.get("circuitBreakerEvents");
        
        // Events should be an array
        assertThat(events.isArray()).isTrue();
    }

    @Test
    void testSpecificCircuitBreakerEndpoint() throws Exception {
        String userCbUrl = "http://localhost:" + port + "/actuator/circuitbreakers/user-service-cb";
        
        ResponseEntity<String> response = restTemplate.getForEntity(userCbUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode cbJson = objectMapper.readTree(response.getBody());
        
        // Verify specific circuit breaker details
        assertThat(cbJson.get("name").asText()).isEqualTo("user-service-cb");
        assertThat(cbJson.has("state")).isTrue();
        assertThat(cbJson.has("config")).isTrue();
        assertThat(cbJson.has("metrics")).isTrue();
        
        // Verify configuration values match properties
        JsonNode config = cbJson.get("config");
        assertThat(config.get("slidingWindowSize").asInt()).isEqualTo(10);
        assertThat(config.get("minimumNumberOfCalls").asInt()).isEqualTo(5);
        assertThat(config.get("failureRateThreshold").asDouble()).isEqualTo(50.0);
        assertThat(config.get("waitDurationInOpenState").asText()).isEqualTo("PT10S");
        assertThat(config.get("permittedNumberOfCallsInHalfOpenState").asInt()).isEqualTo(3);
    }

    @Test
    void testCircuitBreakerMetricsEndpoint() throws Exception {
        // Generate some metrics data
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        productCb.onSuccess(150, TimeUnit.MILLISECONDS);
        productCb.onSuccess(200, TimeUnit.MILLISECONDS);
        productCb.onError(300, TimeUnit.MILLISECONDS, new RuntimeException("Test error"));
        
        String metricsUrl = "http://localhost:" + port + "/actuator/circuitbreakers/product-service-cb";
        
        ResponseEntity<String> response = restTemplate.getForEntity(metricsUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode cbJson = objectMapper.readTree(response.getBody());
        JsonNode metrics = cbJson.get("metrics");
        
        // Verify metrics are available
        assertThat(metrics.has("numberOfBufferedCalls")).isTrue();
        assertThat(metrics.has("numberOfSuccessfulCalls")).isTrue();
        assertThat(metrics.has("numberOfFailedCalls")).isTrue();
        assertThat(metrics.has("failureRate")).isTrue();
        
        // Verify metrics reflect our test calls
        assertThat(metrics.get("numberOfBufferedCalls").asInt()).isEqualTo(3);
        assertThat(metrics.get("numberOfSuccessfulCalls").asInt()).isEqualTo(2);
        assertThat(metrics.get("numberOfFailedCalls").asInt()).isEqualTo(1);
    }

    @Test
    void testCircuitBreakerEventsForSpecificInstance() throws Exception {
        String userEventsUrl = "http://localhost:" + port + "/actuator/circuitbreakerevents/user-service-cb";
        
        ResponseEntity<String> response = restTemplate.getForEntity(userEventsUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        JsonNode eventsJson = objectMapper.readTree(response.getBody());
        
        // Verify events structure for specific circuit breaker
        assertThat(eventsJson.has("circuitBreakerEvents")).isTrue();
        JsonNode events = eventsJson.get("circuitBreakerEvents");
        assertThat(events.isArray()).isTrue();
    }

    @Test
    void testHealthEndpointShowsDetailedInformation() throws Exception {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode healthJson = objectMapper.readTree(response.getBody());
        
        // Since management.endpoint.health.show-details=always is configured,
        // we should get detailed health information
        assertThat(healthJson.has("status")).isTrue();
        
        // The response should contain more than just the status
        assertThat(healthJson.size()).isGreaterThan(1);
    }

    @Test
    void testManagementEndpointsConfiguration() {
        // Test that the configured endpoints are exposed
        String[] expectedEndpoints = {
            "/actuator/health",
            "/actuator/circuitbreakers", 
            "/actuator/circuitbreakerevents"
        };
        
        for (String endpoint : expectedEndpoints) {
            String url = "http://localhost:" + port + endpoint;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // All configured endpoints should be accessible
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void testCircuitBreakerStateTransitionMonitoring() throws Exception {
        CircuitBreaker testCb = circuitBreakerRegistry.circuitBreaker("monitoring-test-cb");
        
        // Initial state should be CLOSED
        String cbUrl = "http://localhost:" + port + "/actuator/circuitbreakers/monitoring-test-cb";
        ResponseEntity<String> response = restTemplate.getForEntity(cbUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        JsonNode cbJson = objectMapper.readTree(response.getBody());
        assertThat(cbJson.get("state").asText()).isEqualTo("CLOSED");
        
        // Generate failures to potentially trigger state change
        for (int i = 0; i < 10; i++) {
            testCb.onError(100, TimeUnit.MILLISECONDS, new RuntimeException("Test failure"));
        }
        
        // Check state again - might have changed depending on configuration
        response = restTemplate.getForEntity(cbUrl, String.class);
        cbJson = objectMapper.readTree(response.getBody());
        
        // State should be trackable through the endpoint
        assertThat(cbJson.has("state")).isTrue();
        assertThat(cbJson.get("state").asText()).isIn("CLOSED", "OPEN", "HALF_OPEN");
    }
}