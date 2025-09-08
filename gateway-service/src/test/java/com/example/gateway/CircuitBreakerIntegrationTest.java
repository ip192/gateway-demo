package com.example.gateway;

import com.example.gateway.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for circuit breaker functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CircuitBreakerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Start WireMock servers for user and product services with dynamic ports
        userServiceMock = new WireMockServer(0); // Use dynamic port
        productServiceMock = new WireMockServer(0); // Use dynamic port
        
        userServiceMock.start();
        productServiceMock.start();
        
        WireMock.configureFor("localhost", userServiceMock.port());
    }

    @AfterEach
    void tearDown() {
        if (userServiceMock != null && userServiceMock.isRunning()) {
            userServiceMock.stop();
        }
        if (productServiceMock != null && productServiceMock.isRunning()) {
            productServiceMock.stop();
        }
    }

    @Test
    void testCircuitBreakerTriggersAfterFailures() throws Exception {
        // Configure user service to return 500 errors
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple requests to trigger circuit breaker
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/user/1", String.class);
            
            // After enough failures, circuit breaker should open and return fallback
            if (i >= 5) {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                
                ApiResponse<?> apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
                assertThat(apiResponse.isSuccess()).isFalse();
                assertThat(apiResponse.getMessage()).contains("temporarily unavailable");
            }
        }
    }

    @Test
    void testCircuitBreakerFallbackForUserService() {
        // Configure user service to be unavailable
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(6000))); // Timeout to trigger circuit breaker

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should get fallback response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("User service is temporarily unavailable");
    }

    @Test
    void testCircuitBreakerFallbackForProductService() {
        // Configure product service to be unavailable
        WireMock.configureFor("localhost", productServiceMock.port());
        productServiceMock.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(6000))); // Timeout to trigger circuit breaker

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/product/1", String.class);

        // Should get fallback response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("Product service is temporarily unavailable");
    }

    @Test
    void testCircuitBreakerRecoveryAfterServiceReturns() throws Exception {
        // First, make service fail to open circuit breaker
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)));

        String gatewayUrl = "http://localhost:" + port;

        // Trigger circuit breaker
        for (int i = 0; i < 8; i++) {
            restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);
        }

        // Now make service healthy again
        userServiceMock.resetAll();
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        // Wait for circuit breaker to transition to half-open
        Thread.sleep(12000);

        // Make a request - should succeed and close circuit breaker
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Test User");
    }

    @Test
    void testSuccessfulRequestsDoNotTriggerCircuitBreaker() {
        // Configure user service to return successful responses
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple successful requests
        for (int i = 0; i < 20; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/user/1", String.class);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Test User");
        }
    }
}