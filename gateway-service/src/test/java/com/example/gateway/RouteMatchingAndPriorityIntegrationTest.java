package com.example.gateway;

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
import org.springframework.test.context.TestPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for route matching rules and priority handling
 * Tests various path patterns, route priorities, and matching logic
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Define multiple routes with different priorities and patterns
    "spring.cloud.gateway.routes[0].id=exact-match-route",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8091",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/api/exact",
    "spring.cloud.gateway.routes[0].order=1",
    
    "spring.cloud.gateway.routes[1].id=prefix-match-route",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8092",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/api/prefix/**",
    "spring.cloud.gateway.routes[1].order=2",
    
    "spring.cloud.gateway.routes[2].id=wildcard-match-route",
    "spring.cloud.gateway.routes[2].uri=http://localhost:8093",
    "spring.cloud.gateway.routes[2].predicates[0]=Path=/api/**",
    "spring.cloud.gateway.routes[2].order=3",
    
    "spring.cloud.gateway.routes[3].id=method-specific-route",
    "spring.cloud.gateway.routes[3].uri=http://localhost:8094",
    "spring.cloud.gateway.routes[3].predicates[0]=Path=/api/method/**",
    "spring.cloud.gateway.routes[3].predicates[1]=Method=POST",
    "spring.cloud.gateway.routes[3].order=1",
    
    "spring.cloud.gateway.routes[4].id=header-based-route",
    "spring.cloud.gateway.routes[4].uri=http://localhost:8095",
    "spring.cloud.gateway.routes[4].predicates[0]=Path=/api/header/**",
    "spring.cloud.gateway.routes[4].predicates[1]=Header=X-Version,v2",
    "spring.cloud.gateway.routes[4].order=1",
    
    "spring.cloud.gateway.routes[5].id=query-param-route",
    "spring.cloud.gateway.routes[5].uri=http://localhost:8096",
    "spring.cloud.gateway.routes[5].predicates[0]=Path=/api/query/**",
    "spring.cloud.gateway.routes[5].predicates[1]=Query=version,beta",
    "spring.cloud.gateway.routes[5].order=1"
})
public class RouteMatchingAndPriorityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer exactMatchService;
    private WireMockServer prefixMatchService;
    private WireMockServer wildcardMatchService;
    private WireMockServer methodSpecificService;
    private WireMockServer headerBasedService;
    private WireMockServer queryParamService;

    @BeforeEach
    void setUp() {
        // Start all mock services
        exactMatchService = new WireMockServer(8091);
        prefixMatchService = new WireMockServer(8092);
        wildcardMatchService = new WireMockServer(8093);
        methodSpecificService = new WireMockServer(8094);
        headerBasedService = new WireMockServer(8095);
        queryParamService = new WireMockServer(8096);

        exactMatchService.start();
        prefixMatchService.start();
        wildcardMatchService.start();
        methodSpecificService.start();
        headerBasedService.start();
        queryParamService.start();

        setupMockServices();
    }

    @AfterEach
    void tearDown() {
        stopService(exactMatchService);
        stopService(prefixMatchService);
        stopService(wildcardMatchService);
        stopService(methodSpecificService);
        stopService(headerBasedService);
        stopService(queryParamService);
    }

    @Test
    void testExactPathMatching() {
        String gatewayUrl = "http://localhost:" + port;

        // Test exact match
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/api/exact", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("exact-match-service");
    }

    @Test
    void testPrefixPathMatching() {
        String gatewayUrl = "http://localhost:" + port;

        // Test prefix match
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/api/prefix/test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("prefix-match-service");

        // Test deeper prefix match
        ResponseEntity<String> deepResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/prefix/deep/path", String.class);

        assertThat(deepResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deepResponse.getBody()).contains("prefix-match-service");
    }

    @Test
    void testWildcardPathMatching() {
        String gatewayUrl = "http://localhost:" + port;

        // Test wildcard match for paths not matched by more specific routes
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/api/wildcard/test", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("wildcard-match-service");
    }

    @Test
    void testRoutePriorityOrdering() {
        String gatewayUrl = "http://localhost:" + port;

        // Test that exact match (order=1) takes priority over wildcard (order=3)
        ResponseEntity<String> exactResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/exact", String.class);

        assertThat(exactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exactResponse.getBody()).contains("exact-match-service");
        assertThat(exactResponse.getBody()).doesNotContain("wildcard-match-service");

        // Test that prefix match (order=2) takes priority over wildcard (order=3)
        ResponseEntity<String> prefixResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/prefix/test", String.class);

        assertThat(prefixResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(prefixResponse.getBody()).contains("prefix-match-service");
        assertThat(prefixResponse.getBody()).doesNotContain("wildcard-match-service");
    }

    @Test
    void testMethodSpecificRouting() {
        String gatewayUrl = "http://localhost:" + port;

        // Test POST method matches method-specific route
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                gatewayUrl + "/api/method/test", null, String.class);

        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody()).contains("method-specific-service");

        // Test GET method falls back to wildcard route
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/method/test", String.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).contains("wildcard-match-service");
    }

    @Test
    void testHeaderBasedRouting() {
        String gatewayUrl = "http://localhost:" + port;

        // Test with correct header
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Version", "v2");
        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        ResponseEntity<String> headerResponse = restTemplate.exchange(
                gatewayUrl + "/api/header/test", 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                String.class);

        assertThat(headerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(headerResponse.getBody()).contains("header-based-service");

        // Test without header - should fall back to wildcard route
        ResponseEntity<String> noHeaderResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/header/test", String.class);

        assertThat(noHeaderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noHeaderResponse.getBody()).contains("wildcard-match-service");
    }

    @Test
    void testQueryParameterBasedRouting() {
        String gatewayUrl = "http://localhost:" + port;

        // Test with correct query parameter
        ResponseEntity<String> queryResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/query/test?version=beta", String.class);

        assertThat(queryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(queryResponse.getBody()).contains("query-param-service");

        // Test without query parameter - should fall back to wildcard route
        ResponseEntity<String> noQueryResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/query/test", String.class);

        assertThat(noQueryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(noQueryResponse.getBody()).contains("wildcard-match-service");

        // Test with wrong query parameter value
        ResponseEntity<String> wrongQueryResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/query/test?version=alpha", String.class);

        assertThat(wrongQueryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(wrongQueryResponse.getBody()).contains("wildcard-match-service");
    }

    @Test
    void testComplexRoutePriorityScenario() {
        String gatewayUrl = "http://localhost:" + port;

        // Test that method-specific route (order=1) takes priority over prefix route (order=2)
        // for POST /api/prefix/method-test
        ResponseEntity<String> postResponse = restTemplate.postForEntity(
                gatewayUrl + "/api/method/prefix-test", null, String.class);

        // Should match method-specific route because it has higher priority (order=1)
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody()).contains("method-specific-service");
    }

    @Test
    void testUnmatchedRoutes() {
        String gatewayUrl = "http://localhost:" + port;

        // Test completely unmatched path
        ResponseEntity<String> response = restTemplate.getForEntity(
                gatewayUrl + "/unmatched/path", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testCaseInsensitivePathMatching() {
        String gatewayUrl = "http://localhost:" + port;

        // Test case sensitivity - Spring Cloud Gateway paths are case-sensitive by default
        ResponseEntity<String> upperCaseResponse = restTemplate.getForEntity(
                gatewayUrl + "/API/EXACT", String.class);

        assertThat(upperCaseResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // Helper methods

    private void setupMockServices() {
        setupMockService(exactMatchService, "exact-match-service");
        setupMockService(prefixMatchService, "prefix-match-service");
        setupMockService(wildcardMatchService, "wildcard-match-service");
        setupMockService(methodSpecificService, "method-specific-service");
        setupMockService(headerBasedService, "header-based-service");
        setupMockService(queryParamService, "query-param-service");
    }

    private void setupMockService(WireMockServer server, String serviceName) {
        WireMock.configureFor("localhost", server.port());
        server.stubFor(any(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"service\": \"" + serviceName + "\", \"message\": \"Response from " + serviceName + "\"}")));
    }

    private void stopService(WireMockServer server) {
        if (server != null && server.isRunning()) {
            server.stop();
        }
    }
}