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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration tests for error handling and fallback mechanisms
 * Tests various error scenarios and validates fallback responses
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=user-service-error-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8091",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[0].filters[0].args.name=user-service-cb",
    "spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/user",
    "spring.cloud.gateway.routes[0].filters[1].name=Retry",
    "spring.cloud.gateway.routes[0].filters[1].args.retries=3",
    
    "spring.cloud.gateway.routes[1].id=product-service-error-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8092",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[1].filters[0].args.name=product-service-cb",
    "spring.cloud.gateway.routes[1].filters[0].args.fallbackUri=forward:/fallback/product",
    "spring.cloud.gateway.routes[1].filters[1].name=Retry",
    "spring.cloud.gateway.routes[1].filters[1].args.retries=2",
    
    // Circuit breaker configuration
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=5",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=60",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=5s",
    
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=5",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=60",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=5s",
    
    // Timeout configuration
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=2s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=2s"
})
public class ErrorHandlingAndFallbackIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer userService;
    private WireMockServer productService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userService = new WireMockServer(8091);
        productService = new WireMockServer(8092);
        
        userService.start();
        productService.start();
    }

    @AfterEach
    void tearDown() {
        if (userService != null && userService.isRunning()) {
            userService.stop();
        }
        if (productService != null && productService.isRunning()) {
            productService.stop();
        }
    }

    @Test
    void testServiceUnavailableFallback() {
        // Configure user service to be completely unavailable
        userService.stop();

        String gatewayUrl = "http://localhost:" + port;
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should get error response (either 500 or 503 depending on gateway behavior)
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        // The response might be a fallback or an error response
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void testTimeoutFallback() throws Exception {
        // Configure user service to respond slowly (timeout)
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5000) // 5 second delay, timeout is 2s
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        String gatewayUrl = "http://localhost:" + port;
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        // Should get error response due to timeout
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void testServerErrorFallback() {
        // Configure user service to return 500 errors
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple requests to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/user/1", String.class);
            
            // After enough failures, should get error response
            if (i >= 3) {
                assertThat(response.getStatusCode().is5xxServerError()).isTrue();
                assertThat(response.getBody()).isNotEmpty();
            }
        }
    }

    @Test
    void testRetryMechanism() {
        // Configure user service to fail first few times, then succeed
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlEqualTo("/user/retry-test"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}"))
                .willSetStateTo("First Retry"));

        userService.stubFor(get(urlEqualTo("/user/retry-test"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Retry")
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service Unavailable\"}"))
                .willSetStateTo("Second Retry"));

        userService.stubFor(get(urlEqualTo("/user/retry-test"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Success after retry\"}")));

        String gatewayUrl = "http://localhost:" + port;
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/retry-test", String.class);

        // Should eventually succeed after retries
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Success after retry");
    }

    @Test
    void testCircuitBreakerOpenState() throws Exception {
        // Configure user service to always fail
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Internal Server Error\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make enough requests to open circuit breaker
        for (int i = 0; i < 6; i++) {
            restTemplate.getForEntity(gatewayUrl + "/user/cb-test", String.class);
        }

        // Circuit breaker should now be open, requests should fail fast
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/cb-test", String.class);
        long endTime = System.currentTimeMillis();

        // Should get fallback response quickly (circuit breaker open)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(endTime - startTime).isLessThan(1000); // Should be fast
        assertThat(response.getBody()).contains("temporarily unavailable");
    }

    @Test
    void testCircuitBreakerRecovery() throws Exception {
        // First, open the circuit breaker
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(500)));

        String gatewayUrl = "http://localhost:" + port;

        // Trigger circuit breaker
        for (int i = 0; i < 6; i++) {
            restTemplate.getForEntity(gatewayUrl + "/user/recovery-test", String.class);
        }

        // Now make service healthy
        userService.resetAll();
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Service Recovered\"}")));

        // Wait for circuit breaker to transition to half-open
        Thread.sleep(6000);

        // Make a request - should succeed and close circuit breaker
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/recovery-test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Service Recovered");
    }

    @Test
    void testDifferentServiceFallbacks() {
        // Test that different services have different fallback responses
        userService.stop();
        productService.stop();

        String gatewayUrl = "http://localhost:" + port;

        // Test user service fallback
        ResponseEntity<String> userResponse = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(userResponse.getBody()).contains("User service");

        // Test product service fallback
        ResponseEntity<String> productResponse = restTemplate.getForEntity(
                gatewayUrl + "/product/1", String.class);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(productResponse.getBody()).contains("Product service");

        // Verify they have different messages
        assertThat(userResponse.getBody()).isNotEqualTo(productResponse.getBody());
    }

    @Test
    void testPostRequestFallback() {
        // Test fallback for POST requests
        userService.stop();

        String gatewayUrl = "http://localhost:" + port;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"name\":\"test\"}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
                gatewayUrl + "/user/create", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("User service is temporarily unavailable");
    }

    @Test
    void testFallbackWithCustomHeaders() {
        // Test that fallback responses include proper headers
        userService.stop();

        String gatewayUrl = "http://localhost:" + port;
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        
        // Check for custom headers that might be added by fallback controller
        assertThat(response.getHeaders().get("X-Fallback-Reason")).isNotNull();
    }

    @Test
    void testErrorResponseFormat() throws Exception {
        // Test that error responses follow consistent format
        userService.stop();

        String gatewayUrl = "http://localhost:" + port;
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/user/1", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // Parse response as ApiResponse
        ApiResponse<?> apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
        assertThat(apiResponse.isSuccess()).isFalse();
        assertThat(apiResponse.getMessage()).isNotEmpty();
        assertThat(apiResponse.getData()).isNull();
    }

    @Test
    void testSuccessfulRequestsDoNotTriggerFallback() {
        // Configure services to work normally
        WireMock.configureFor("localhost", userService.port());
        userService.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test User\"}")));

        WireMock.configureFor("localhost", productService.port());
        productService.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Test Product\"}")));

        String gatewayUrl = "http://localhost:" + port;

        // Make multiple successful requests
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> userResponse = restTemplate.getForEntity(
                    gatewayUrl + "/user/" + i, String.class);
            assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(userResponse.getBody()).contains("Test User");

            ResponseEntity<String> productResponse = restTemplate.getForEntity(
                    gatewayUrl + "/product/" + i, String.class);
            assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(productResponse.getBody()).contains("Test Product");
        }
    }
}