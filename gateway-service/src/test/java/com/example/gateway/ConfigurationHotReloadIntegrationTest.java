package com.example.gateway;

import com.example.gateway.config.GatewayRoutingProperties;
import com.example.gateway.model.PredicateConfig;
import com.example.gateway.model.RouteConfig;
import com.example.gateway.model.RouteMetadata;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.time.Duration.ofSeconds;

/**
 * Integration tests for configuration hot reload functionality
 * Tests the ability to dynamically update routing configuration without restarting the gateway
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=refresh,health,gateway",
    "spring.cloud.gateway.discovery.locator.enabled=false"
})
public class ConfigurationHotReloadIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GatewayRoutingProperties gatewayRoutingProperties;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ContextRefresher contextRefresher;

    private WireMockServer mockService1;
    private WireMockServer mockService2;

    @BeforeEach
    void setUp() {
        // Start mock services
        mockService1 = new WireMockServer(8091);
        mockService2 = new WireMockServer(8092);
        
        mockService1.start();
        mockService2.start();

        // Configure mock responses
        setupMockService1();
        setupMockService2();
    }

    @AfterEach
    void tearDown() {
        if (mockService1 != null && mockService1.isRunning()) {
            mockService1.stop();
        }
        if (mockService2 != null && mockService2.isRunning()) {
            mockService2.stop();
        }
    }

    @Test
    void testAddNewRouteViaConfigurationRefresh() {
        String gatewayUrl = "http://localhost:" + port;

        // Initially, /api/v2/** should not be routed
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/v2/test", String.class);
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Add new route configuration programmatically
        addNewRouteConfiguration();

        // Trigger configuration refresh
        refreshConfiguration();

        // Wait for route to be available
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/api/v2/test", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Service 2 Response");
        });
    }

    @Test
    void testModifyExistingRouteConfiguration() {
        String gatewayUrl = "http://localhost:" + port;

        // Initially, /api/v1/** should route to service 1
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/v1/test", String.class);
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initialResponse.getBody()).contains("Service 1 Response");

        // Modify route to point to service 2
        modifyExistingRouteConfiguration();

        // Trigger configuration refresh
        refreshConfiguration();

        // Wait for route modification to take effect
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/api/v1/test", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Service 2 Response");
        });
    }

    @Test
    void testDisableRouteViaConfiguration() {
        String gatewayUrl = "http://localhost:" + port;

        // Initially, route should be available
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
                gatewayUrl + "/api/v1/test", String.class);
        assertThat(initialResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Disable the route
        disableRouteConfiguration();

        // Trigger configuration refresh
        refreshConfiguration();

        // Wait for route to be disabled
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/api/v1/test", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        });
    }

    @Test
    void testRouteOrderChangeViaConfiguration() {
        String gatewayUrl = "http://localhost:" + port;

        // Add overlapping routes with different orders
        addOverlappingRoutes();
        refreshConfiguration();

        // Test that higher priority route is matched first
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/api/priority/test", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("High Priority Service");
        });

        // Change route order
        changeRouteOrder();
        refreshConfiguration();

        // Test that route order change takes effect
        await().atMost(ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    gatewayUrl + "/api/priority/test", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("Low Priority Service");
        });
    }

    @Test
    void testConfigurationRefreshEndpoint() {
        // Test that the refresh endpoint is available and working
        String refreshUrl = "http://localhost:" + port + "/actuator/refresh";
        ResponseEntity<String> response = restTemplate.postForEntity(refreshUrl, null, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGatewayRoutesEndpoint() {
        // Test that we can view current routes via actuator endpoint
        String routesUrl = "http://localhost:" + port + "/actuator/gateway/routes";
        ResponseEntity<String> response = restTemplate.getForEntity(routesUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    // Helper methods

    private void setupMockService1() {
        WireMock.configureFor("localhost", mockService1.port());
        mockService1.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service 1 Response\", \"service\": \"service1\"}")));
    }

    private void setupMockService2() {
        WireMock.configureFor("localhost", mockService2.port());
        mockService2.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service 2 Response\", \"service\": \"service2\"}")));
    }

    private void addNewRouteConfiguration() {
        RouteConfig newRoute = new RouteConfig();
        newRoute.setId("service2-route");
        newRoute.setUri("http://localhost:" + mockService2.port());
        
        PredicateConfig pathPredicate = new PredicateConfig();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("pattern", "/api/v2/**");
        pathPredicate.setArgs(args);
        newRoute.setPredicates(Arrays.asList(pathPredicate));
        
        RouteMetadata metadata = new RouteMetadata();
        metadata.setEnabled(true);
        metadata.setOrder(1);
        newRoute.setMetadata(metadata);

        List<RouteConfig> currentRoutes = new ArrayList<>(gatewayRoutingProperties.getRoutes());
        currentRoutes.add(newRoute);
        gatewayRoutingProperties.setRoutes(currentRoutes);
    }

    private void modifyExistingRouteConfiguration() {
        List<RouteConfig> routes = gatewayRoutingProperties.getRoutes();
        for (RouteConfig route : routes) {
            if ("service1-route".equals(route.getId())) {
                route.setUri("http://localhost:" + mockService2.port());
                break;
            }
        }
    }

    private void disableRouteConfiguration() {
        List<RouteConfig> routes = gatewayRoutingProperties.getRoutes();
        for (RouteConfig route : routes) {
            if ("service1-route".equals(route.getId())) {
                route.getMetadata().setEnabled(false);
                break;
            }
        }
    }

    private void addOverlappingRoutes() {
        // Add high priority route
        RouteConfig highPriorityRoute = new RouteConfig();
        highPriorityRoute.setId("high-priority-route");
        highPriorityRoute.setUri("http://localhost:" + mockService1.port());
        
        PredicateConfig pathPredicate1 = new PredicateConfig();
        pathPredicate1.setName("Path");
        Map<String, String> args1 = new HashMap<>();
        args1.put("pattern", "/api/priority/**");
        pathPredicate1.setArgs(args1);
        highPriorityRoute.setPredicates(Arrays.asList(pathPredicate1));
        
        RouteMetadata metadata1 = new RouteMetadata();
        metadata1.setEnabled(true);
        metadata1.setOrder(1);
        highPriorityRoute.setMetadata(metadata1);

        // Add low priority route
        RouteConfig lowPriorityRoute = new RouteConfig();
        lowPriorityRoute.setId("low-priority-route");
        lowPriorityRoute.setUri("http://localhost:" + mockService2.port());
        
        PredicateConfig pathPredicate2 = new PredicateConfig();
        pathPredicate2.setName("Path");
        Map<String, String> args2 = new HashMap<>();
        args2.put("pattern", "/api/**");
        pathPredicate2.setArgs(args2);
        lowPriorityRoute.setPredicates(Arrays.asList(pathPredicate2));
        
        RouteMetadata metadata2 = new RouteMetadata();
        metadata2.setEnabled(true);
        metadata2.setOrder(2);
        lowPriorityRoute.setMetadata(metadata2);

        // Mock service 1 to return high priority response
        WireMock.configureFor("localhost", mockService1.port());
        mockService1.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"High Priority Service\", \"priority\": \"high\"}")));

        // Mock service 2 to return low priority response
        WireMock.configureFor("localhost", mockService2.port());
        mockService2.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Low Priority Service\", \"priority\": \"low\"}")));

        List<RouteConfig> currentRoutes = new ArrayList<>(gatewayRoutingProperties.getRoutes());
        currentRoutes.add(highPriorityRoute);
        currentRoutes.add(lowPriorityRoute);
        gatewayRoutingProperties.setRoutes(currentRoutes);
    }

    private void changeRouteOrder() {
        List<RouteConfig> routes = gatewayRoutingProperties.getRoutes();
        for (RouteConfig route : routes) {
            if ("high-priority-route".equals(route.getId())) {
                route.getMetadata().setOrder(3); // Lower priority
            } else if ("low-priority-route".equals(route.getId())) {
                route.getMetadata().setOrder(1); // Higher priority
            }
        }
    }

    private void refreshConfiguration() {
        contextRefresher.refresh();
    }
}