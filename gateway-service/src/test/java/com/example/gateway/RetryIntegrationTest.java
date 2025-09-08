package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
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
 * Integration tests for retry functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RetryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;

    @BeforeEach
    void setUp() {
        // Start WireMock servers for user and product services
        userServiceMock = new WireMockServer(8081);
        productServiceMock = new WireMockServer(8082);
        
        userServiceMock.start();
        productServiceMock.start();
        
        WireMock.configureFor("localhost", 8081);
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
    void testRetryOnServiceUnavailable() {
        // Configure user service to return 503 (Service Unavailable) which should trigger retry
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY);

        // Verify that multiple requests were made (original + retries)
        userServiceMock.verify(moreThan(1), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testRetryOnBadGateway() {
        // Configure user service to return 502 (Bad Gateway) which should trigger retry
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(502)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Bad Gateway\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE);

        // Verify that multiple requests were made (original + retries)
        userServiceMock.verify(moreThan(1), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testRetrySucceedsAfterFailures() {
        // Configure user service to fail first 2 times, then succeed
        userServiceMock.stubFor(get(urlPathEqualTo("/user/1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}"))
                .willSetStateTo("First Retry"));

        userServiceMock.stubFor(get(urlPathEqualTo("/user/1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}"))
                .willSetStateTo("Second Retry"));

        userServiceMock.stubFor(get(urlPathEqualTo("/user/1"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should succeed after retries
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Test User");

        // Verify that exactly 3 requests were made (original + 2 retries)
        userServiceMock.verify(exactly(3), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testNoRetryOnClientError() {
        // Configure user service to return 400 (Bad Request) which should NOT trigger retry
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Bad Request\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should fail immediately without retries
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify that only one request was made (no retries)
        userServiceMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testRetryWithBackoffDelay() throws InterruptedException {
        // Configure user service to always return 503
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        String gatewayUrl = "http://localhost:" + port;

        long startTime = System.currentTimeMillis();
        
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Should take some time due to backoff delays between retries
        // With 3 retries and exponential backoff starting at 50ms, should take at least 150ms
        assertThat(totalTime).isGreaterThan(100);

        // Verify that multiple requests were made
        userServiceMock.verify(moreThan(1), getRequestedFor(urlPathEqualTo("/user/1")));
    }

    @Test
    void testRetryForProductService() {
        // Configure product service to return 503
        WireMock.configureFor("localhost", 8082);
        productServiceMock.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/product/1", String.class);

        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY);

        // Verify that multiple requests were made (original + retries)
        productServiceMock.verify(moreThan(1), getRequestedFor(urlPathEqualTo("/product/1")));
    }

    @Test
    void testSuccessfulRequestsDoNotRetry() {
        // Configure user service to return successful responses
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        String gatewayUrl = "http://localhost:" + port;

        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should succeed immediately
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Test User");

        // Verify that only one request was made (no retries needed)
        userServiceMock.verify(exactly(1), getRequestedFor(urlPathEqualTo("/user/1")));
    }
}