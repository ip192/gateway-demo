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
 * Integration tests for circuit breaker and retry working together
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CircuitBreakerRetryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer userServiceMock;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userServiceMock = new WireMockServer(8081);
        userServiceMock.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void tearDown() {
        if (userServiceMock != null && userServiceMock.isRunning()) {
            userServiceMock.stop();
        }
    }

    @Test
    void testRetryThenCircuitBreakerFallback() throws Exception {
        // Configure service to always fail with 503 (triggers retry)
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple requests to eventually trigger circuit breaker
        ResponseEntity<String> lastResponse = null;
        for (int i = 0; i < 15; i++) {
            lastResponse = restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);
            
            // Early requests should retry and then fail
            // Later requests should trigger circuit breaker fallback
            if (i < 5) {
                // Should be retrying and failing
                assertThat(lastResponse.getStatusCode()).isIn(
                    HttpStatus.SERVICE_UNAVAILABLE, 
                    HttpStatus.BAD_GATEWAY
                );
            } else {
                // Circuit breaker should be open, returning fallback
                assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                
                if (lastResponse.getBody() != null && lastResponse.getBody().contains("temporarily unavailable")) {
                    // This is the fallback response
                    ApiResponse<?> apiResponse = objectMapper.readValue(lastResponse.getBody(), ApiResponse.class);
                    assertThat(apiResponse.isSuccess()).isFalse();
                    assertThat(apiResponse.getMessage()).contains("temporarily unavailable");
                    break;
                }
            }
            
            // Small delay between requests
            Thread.sleep(100);
        }

        // Verify that retries were attempted initially
        // The exact count may vary due to circuit breaker timing
        userServiceMock.verify(moreThan(3), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testCircuitBreakerPreventsUnnecessaryRetries() throws Exception {
        // First, trigger circuit breaker with multiple failures
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make requests to open circuit breaker
        for (int i = 0; i < 10; i++) {
            restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);
            Thread.sleep(50);
        }

        // Reset request count
        userServiceMock.resetRequests();

        // Now make a request when circuit breaker is open
        ResponseEntity<String> response = restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);

        // Should get fallback immediately without retries
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // Verify that no requests were made to the downstream service
        // (circuit breaker prevented the call)
        userServiceMock.verify(exactly(0), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testRetrySuccessPreventCircuitBreakerOpening() {
        // Configure service to fail first attempt but succeed on retry
        userServiceMock.stubFor(get(urlPathEqualTo("/user/1"))
                .inScenario("Success on Retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}"))
                .willSetStateTo("Retry"));

        userServiceMock.stubFor(get(urlPathEqualTo("/user/1"))
                .inScenario("Success on Retry")
                .whenScenarioStateIs("Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple requests - they should succeed due to retry
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);
            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Test User");
            
            // Reset scenario for next request
            userServiceMock.resetScenarios();
        }

        // Circuit breaker should not have opened because retries were successful
        // All requests should have succeeded
    }

    @Test
    void testTimeoutTriggersCircuitBreakerAndFallback() {
        // Configure service with long delay to trigger timeout
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")
                        .withFixedDelay(8000))); // Longer than timeout

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple requests to trigger circuit breaker due to timeouts
        for (int i = 0; i < 8; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(gatewayUrl + "/user/1", String.class);
            
            // Should eventually get fallback response
            if (i >= 5) {
                assertThat(response.getStatusCode()).isIn(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    HttpStatus.REQUEST_TIMEOUT,
                    HttpStatus.GATEWAY_TIMEOUT
                );
            }
        }
    }
}