package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HTTP client timeout configuration from properties files
 * Verifies that connect and response timeouts are properly configured and enforced
 * Requirements: 6.1, 6.2
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.httpclient.connect-timeout=2000",
    "spring.cloud.gateway.httpclient.response-timeout=3s",
    "spring.cloud.gateway.routes[0].id=timeout-test-route",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8091",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/timeout/**"
})
class HttpClientTimeoutPropertiesTest {

    @Autowired
    private HttpClientProperties httpClientProperties;

    @Autowired
    private WebTestClient webTestClient;

    private WireMockServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = new WireMockServer(WireMockConfiguration.options().port(8091));
        mockServer.start();
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @Test
    void testConnectTimeoutPropertyLoaded() {
        // Verify connect timeout is loaded from properties
        assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(2000);
    }

    @Test
    void testResponseTimeoutPropertyLoaded() {
        // Verify response timeout is loaded from properties
        assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void testConnectTimeoutEnforced() {
        // Configure mock to not accept connections (simulate connection timeout)
        mockServer.stop();
        
        // Request should fail due to connect timeout
        webTestClient.get()
                .uri("/timeout/connect")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testResponseTimeoutEnforced() {
        // Configure mock to delay response longer than timeout
        mockServer.stubFor(get(urlPathEqualTo("/timeout/response"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Delayed response")
                        .withFixedDelay(5000))); // 5 second delay, timeout is 3 seconds

        // Request should timeout
        webTestClient.get()
                .uri("/timeout/response")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testSuccessfulRequestWithinTimeout() {
        // Configure mock to respond quickly
        mockServer.stubFor(get(urlPathEqualTo("/timeout/success"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"success\"}")
                        .withFixedDelay(1000))); // 1 second delay, well within timeout

        // Request should succeed
        webTestClient.get()
                .uri("/timeout/success")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isEqualTo("success");
    }

    @Test
    void testTimeoutConfigurationValidation() {
        // Verify timeout values are reasonable
        assertThat(httpClientProperties.getConnectTimeout()).isGreaterThan(0);
        assertThat(httpClientProperties.getConnectTimeout()).isLessThan(30000); // Less than 30 seconds
        
        assertThat(httpClientProperties.getResponseTimeout().toMillis()).isGreaterThan(0);
        assertThat(httpClientProperties.getResponseTimeout().toMillis()).isLessThan(60000L); // Less than 60 seconds
    }

    @Test
    void testTimeoutBoundaryConditions() {
        // Test response just at the timeout boundary
        mockServer.stubFor(get(urlPathEqualTo("/timeout/boundary"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Boundary response")
                        .withFixedDelay(2900))); // Just under 3 second timeout

        // Request should succeed
        webTestClient.get()
                .uri("/timeout/boundary")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testMultipleTimeoutScenarios() {
        // Setup multiple endpoints with different delays
        mockServer.stubFor(get(urlPathEqualTo("/timeout/fast"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Fast response")
                        .withFixedDelay(500)));

        mockServer.stubFor(get(urlPathEqualTo("/timeout/medium"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Medium response")
                        .withFixedDelay(2000)));

        mockServer.stubFor(get(urlPathEqualTo("/timeout/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Slow response")
                        .withFixedDelay(4000))); // Should timeout

        // Test fast response
        webTestClient.get()
                .uri("/timeout/fast")
                .exchange()
                .expectStatus().isOk();

        // Test medium response
        webTestClient.get()
                .uri("/timeout/medium")
                .exchange()
                .expectStatus().isOk();

        // Test slow response (should timeout)
        webTestClient.get()
                .uri("/timeout/slow")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testTimeoutWithDifferentHttpMethods() {
        // Setup mock for different HTTP methods
        mockServer.stubFor(post(urlPathEqualTo("/timeout/post"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("POST success")
                        .withFixedDelay(1000)));

        mockServer.stubFor(put(urlPathEqualTo("/timeout/put"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("PUT success")
                        .withFixedDelay(4000))); // Should timeout

        // Test POST within timeout
        webTestClient.post()
                .uri("/timeout/post")
                .exchange()
                .expectStatus().isOk();

        // Test PUT that should timeout
        webTestClient.put()
                .uri("/timeout/put")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testTimeoutErrorHandling() {
        // Configure mock to timeout
        mockServer.stubFor(get(urlPathEqualTo("/timeout/error"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Should not reach here")
                        .withFixedDelay(5000)));

        // Verify timeout error is handled gracefully
        webTestClient.get()
                .uri("/timeout/error")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    // Should contain timeout-related error information
                    assertThat(body).isNotEmpty();
                });
    }
}