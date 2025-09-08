package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Optional;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.filter.FilterDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for configuration profiles
 * Tests the complete application startup and configuration loading for each profile
 */
class ConfigurationProfilesIntegrationTest {

    /**
     * Integration tests for main application.properties configuration
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    static class MainConfigurationIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void testApplicationStartsSuccessfully() {
            // Verify the application starts and is accessible
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testMainConfigurationRoutesLoaded() {
            // Verify main configuration routes are loaded correctly
            assertThat(gatewayProperties.getRoutes()).hasSize(2);
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-routes".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
            assertThat(userRoute.get().getPredicates()).hasSize(1);
            assertThat(userRoute.get().getFilters()).hasSize(2); // CircuitBreaker + Retry
        }

        @Test
        void testMainConfigurationManagementEndpoints() {
            // Verify management endpoints are exposed correctly
            ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testRouteFiltersConfiguration() {
            // Verify route filters are configured correctly
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-routes".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            
            // Verify circuit breaker filter
            Optional<FilterDefinition> cbFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "CircuitBreaker".equals(filter.getName()))
                .findFirst();
            
            assertThat(cbFilter).isPresent();
            assertThat(cbFilter.get().getArgs()).containsEntry("name", "user-service-cb");
            assertThat(cbFilter.get().getArgs()).containsEntry("fallbackUri", "forward:/fallback/user");
        }

        @Test
        void testRouteMetadataConfiguration() {
            // Verify route metadata is configured correctly
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-routes".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getMetadata()).containsEntry("timeout", "5000");
            assertThat(userRoute.get().getMetadata()).containsEntry("enabled", "true");
            assertThat(userRoute.get().getMetadata()).containsEntry("order", "1");
        }
    }

    /**
     * Integration tests for circuit-breaker profile functionality
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("circuit-breaker")
    static class CircuitBreakerProfileIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void testCircuitBreakerProfileApplicationStartup() {
            // Verify application starts successfully with circuit-breaker profile
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testCircuitBreakerProfileRoutesConfiguration() {
            // Verify circuit-breaker profile routes are loaded
            assertThat(gatewayProperties.getRoutes()).hasSize(2);
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-with-cb".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
            
            // Verify circuit breaker filter is configured
            Optional<FilterDefinition> cbFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "CircuitBreaker".equals(filter.getName()))
                .findFirst();
            
            assertThat(cbFilter).isPresent();
            assertThat(cbFilter.get().getArgs()).containsEntry("name", "user-service-cb");
            assertThat(cbFilter.get().getArgs()).containsEntry("fallbackUri", "forward:/fallback/user");
        }

        @Test
        void testCircuitBreakerSpecificEndpoints() {
            // Verify circuit breaker specific management endpoints
            ResponseEntity<String> circuitBreakersResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/circuitbreakers", String.class);
            assertThat(circuitBreakersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            ResponseEntity<String> circuitBreakerEventsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/circuitbreakerevents", String.class);
            assertThat(circuitBreakerEventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testCircuitBreakerHealthIndicators() {
            // Verify circuit breaker health indicators are enabled
            ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(healthResponse.getBody()).contains("circuitBreakers");
        }

        @Test
        void testRetryFilterConfiguration() {
            // Verify retry filter configuration in circuit-breaker profile
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-with-cb".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            
            Optional<FilterDefinition> retryFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "Retry".equals(filter.getName()))
                .findFirst();
            
            assertThat(retryFilter).isPresent();
            assertThat(retryFilter.get().getArgs()).containsEntry("retries", "3");
        }
    }

    /**
     * Integration tests for performance profile configuration
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("performance")
    static class PerformanceProfileIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Autowired
        private HttpClientProperties httpClientProperties;

        @Test
        void testPerformanceProfileApplicationStartup() {
            // Verify application starts successfully with performance profile
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testPerformanceProfileRoutesConfiguration() {
            // Verify performance profile routes are loaded
            assertThat(gatewayProperties.getRoutes()).hasSize(2);
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
            
            // Verify optimized retry configuration
            Optional<FilterDefinition> retryFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "Retry".equals(filter.getName()))
                .findFirst();
            
            assertThat(retryFilter).isPresent();
            // Performance profile uses fewer retries for faster response
            assertThat(retryFilter.get().getArgs()).containsEntry("retries", "2");
        }

        @Test
        void testPerformanceHttpClientConfiguration() {
            // Verify performance-optimized HTTP client settings
            assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(5000);
            assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(30));
            
            HttpClientProperties.Pool pool = httpClientProperties.getPool();
            assertThat(pool.getMaxConnections()).isEqualTo(500);
            assertThat(pool.getMaxIdleTime()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        void testPerformanceEndpointsConfiguration() {
            // Verify performance monitoring endpoints
            ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);
            assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            ResponseEntity<String> prometheusResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);
            assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testPerformanceMetricsConfiguration() {
            // Verify metrics are enabled and configured
            ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);
            assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(metricsResponse.getBody()).contains("http.server.requests");
        }

        @Test
        void testPerformanceRetryConfiguration() {
            // Verify optimized retry settings in performance profile
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            
            Optional<FilterDefinition> retryFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "Retry".equals(filter.getName()))
                .findFirst();
            
            assertThat(retryFilter).isPresent();
            // Performance profile uses optimized backoff settings
            assertThat(retryFilter.get().getArgs()).containsEntry("backoff.firstBackoff", "10ms");
            assertThat(retryFilter.get().getArgs()).containsEntry("backoff.maxBackoff", "100ms");
        }
    }

    /**
     * Integration tests for dynamic-routing profile functionality
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dynamic-routing")
    static class DynamicRoutingProfileIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void testDynamicRoutingProfileApplicationStartup() {
            // Verify application starts successfully with dynamic-routing profile
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testDynamicRoutingProfileRoutesConfiguration() {
            // Verify dynamic routing profile has multiple route formats
            assertThat(gatewayProperties.getRoutes()).hasSize(5); // Standard + example routes
            
            // Should have both standard and example routes
            Optional<RouteDefinition> standardRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-standard".equals(route.getId()))
                .findFirst();
            
            assertThat(standardRoute).isPresent();
            assertThat(standardRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
        }

        @Test
        void testComplexRouteConfiguration() {
            // Verify complex route with multiple predicates and filters
            Optional<RouteDefinition> complexRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "complex-route-example".equals(route.getId()))
                .findFirst();
            
            assertThat(complexRoute).isPresent();
            assertThat(complexRoute.get().getPredicates()).hasSize(3); // Path, Method, Header
            assertThat(complexRoute.get().getFilters()).hasSize(3); // AddRequestHeader, AddResponseHeader, CircuitBreaker
            
            // Verify specific predicate configurations
            assertThat(complexRoute.get().getPredicates().get(0).getName()).isEqualTo("Path");
            assertThat(complexRoute.get().getPredicates().get(1).getName()).isEqualTo("Method");
            assertThat(complexRoute.get().getPredicates().get(2).getName()).isEqualTo("Header");
        }

        @Test
        void testWeightBasedRouting() {
            // Verify weight-based load balancing routes
            Optional<RouteDefinition> weightedRoute1 = gatewayProperties.getRoutes().stream()
                .filter(route -> "weighted-route-example-1".equals(route.getId()))
                .findFirst();
            
            Optional<RouteDefinition> weightedRoute2 = gatewayProperties.getRoutes().stream()
                .filter(route -> "weighted-route-example-2".equals(route.getId()))
                .findFirst();
            
            assertThat(weightedRoute1).isPresent();
            assertThat(weightedRoute2).isPresent();
            
            // Both should have Weight predicates
            assertThat(weightedRoute1.get().getPredicates()).anyMatch(
                predicate -> predicate.getName().equals("Weight")
            );
            assertThat(weightedRoute2.get().getPredicates()).anyMatch(
                predicate -> predicate.getName().equals("Weight")
            );
            
            // Verify weight values
            assertThat(weightedRoute1.get().getPredicates().stream()
                .filter(p -> "Weight".equals(p.getName()))
                .findFirst().get().getArgs()).containsValue("group1,8");
            assertThat(weightedRoute2.get().getPredicates().stream()
                .filter(p -> "Weight".equals(p.getName()))
                .findFirst().get().getArgs()).containsValue("group1,2");
        }

        @Test
        void testRewritePathFilters() {
            // Verify RewritePath filters in standard format routes
            Optional<RouteDefinition> standardRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-standard".equals(route.getId()))
                .findFirst();
            
            assertThat(standardRoute).isPresent();
            
            Optional<FilterDefinition> rewriteFilter = standardRoute.get().getFilters().stream()
                .filter(filter -> "RewritePath".equals(filter.getName()))
                .findFirst();
            
            assertThat(rewriteFilter).isPresent();
            assertThat(rewriteFilter.get().getArgs()).containsKey("regexp");
            assertThat(rewriteFilter.get().getArgs()).containsKey("replacement");
        }

        @Test
        void testRouteMetadataConfiguration() {
            // Verify route metadata is properly configured
            Optional<RouteDefinition> complexRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "complex-route-example".equals(route.getId()))
                .findFirst();
            
            assertThat(complexRoute).isPresent();
            assertThat(complexRoute.get().getMetadata()).containsEntry("timeout", "8000");
            assertThat(complexRoute.get().getMetadata()).containsEntry("enabled", "true");
            assertThat(complexRoute.get().getMetadata()).containsEntry("order", "200");
        }

        @Test
        void testAddRequestAndResponseHeaderFilters() {
            // Verify AddRequestHeader and AddResponseHeader filters
            Optional<RouteDefinition> complexRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "complex-route-example".equals(route.getId()))
                .findFirst();
            
            assertThat(complexRoute).isPresent();
            
            Optional<FilterDefinition> addRequestHeaderFilter = complexRoute.get().getFilters().stream()
                .filter(filter -> "AddRequestHeader".equals(filter.getName()))
                .findFirst();
            
            Optional<FilterDefinition> addResponseHeaderFilter = complexRoute.get().getFilters().stream()
                .filter(filter -> "AddResponseHeader".equals(filter.getName()))
                .findFirst();
            
            assertThat(addRequestHeaderFilter).isPresent();
            assertThat(addRequestHeaderFilter.get().getArgs()).containsEntry("name", "X-Gateway-Route");
            
            assertThat(addResponseHeaderFilter).isPresent();
            assertThat(addResponseHeaderFilter.get().getArgs()).containsEntry("name", "X-Response-Time");
        }
    }

    /**
     * Integration tests for profile combination scenarios
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({"circuit-breaker", "performance"})
    static class ProfileCombinationIntegrationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Autowired
        private HttpClientProperties httpClientProperties;

        @Test
        void testCombinedProfilesApplicationStartup() {
            // Verify application starts successfully with combined profiles
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testCombinedProfilesRouteConfiguration() {
            // Verify that performance profile routes take precedence (last loaded)
            assertThat(gatewayProperties.getRoutes()).hasSize(2);
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
        }

        @Test
        void testCombinedProfilesHttpClientConfiguration() {
            // Verify HTTP client configuration from performance profile
            assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(5000);
            assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(30));
            
            HttpClientProperties.Pool pool = httpClientProperties.getPool();
            assertThat(pool.getMaxConnections()).isEqualTo(500);
            assertThat(pool.getMaxIdleTime()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        void testCombinedProfilesEndpointsConfiguration() {
            // Verify endpoints from both profiles are available
            ResponseEntity<String> circuitBreakersResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/circuitbreakers", String.class);
            assertThat(circuitBreakersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);
            assertThat(metricsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            ResponseEntity<String> prometheusResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);
            assertThat(prometheusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testProfileOverridesBehavior() {
            // Verify that later profiles override earlier ones
            // Performance profile should override circuit-breaker profile settings
            
            // Check that we don't have circuit-breaker profile specific routes
            Optional<RouteDefinition> cbRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-with-cb".equals(route.getId()))
                .findFirst();
            
            // Should not be present as performance profile overrides it
            assertThat(cbRoute).isEmpty();
            
            // Should have performance profile routes instead
            Optional<RouteDefinition> perfRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(perfRoute).isPresent();
        }
    }

    /**
     * Integration tests for dynamic-routing with circuit-breaker profile combination
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({"circuit-breaker", "dynamic-routing"})
    static class DynamicRoutingCircuitBreakerCombinationTest {

        @LocalServerPort
        private int port;

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void testDynamicRoutingCircuitBreakerCombination() {
            // Verify application starts successfully with dynamic-routing + circuit-breaker profiles
            ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        void testCombinedRoutesConfiguration() {
            // Verify dynamic-routing profile routes are loaded (last profile wins)
            assertThat(gatewayProperties.getRoutes()).hasSize(5);
            
            // Should have dynamic routing routes
            Optional<RouteDefinition> standardRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-standard".equals(route.getId()))
                .findFirst();
            
            assertThat(standardRoute).isPresent();
        }

        @Test
        void testCircuitBreakerEndpointsWithDynamicRouting() {
            // Verify circuit breaker endpoints are still available with dynamic routing
            ResponseEntity<String> circuitBreakersResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/circuitbreakers", String.class);
            assertThat(circuitBreakersResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            ResponseEntity<String> circuitBreakerEventsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/circuitbreakerevents", String.class);
            assertThat(circuitBreakerEventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}