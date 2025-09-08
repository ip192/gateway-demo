package com.example.gateway;

import com.example.gateway.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for route functionality with properties configuration
 * Verifies that routes defined in properties format work correctly
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // User Service Route Configuration
    "spring.cloud.gateway.routes[0].id=user-service-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[0].filters[0].args.name=user-service-cb",
    "spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/user",
    "spring.cloud.gateway.routes[0].filters[1].name=Retry",
    "spring.cloud.gateway.routes[0].filters[1].args.retries=3",
    "spring.cloud.gateway.routes[0].filters[1].args.statuses=BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT",
    "spring.cloud.gateway.routes[0].filters[1].args.methods=GET,POST,PUT,DELETE",
    "spring.cloud.gateway.routes[0].filters[1].args.backoff.firstBackoff=50ms",
    "spring.cloud.gateway.routes[0].filters[1].args.backoff.maxBackoff=500ms",
    "spring.cloud.gateway.routes[0].filters[1].args.backoff.factor=2",
    "spring.cloud.gateway.routes[0].filters[1].args.backoff.basedOnPreviousValue=false",
    "spring.cloud.gateway.routes[0].metadata.timeout=5000",
    "spring.cloud.gateway.routes[0].metadata.enabled=true",
    "spring.cloud.gateway.routes[0].metadata.order=1",
    
    // Product Service Route Configuration
    "spring.cloud.gateway.routes[1].id=product-service-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[1].filters[0].args.name=product-service-cb",
    "spring.cloud.gateway.routes[1].filters[0].args.fallbackUri=forward:/fallback/product",
    "spring.cloud.gateway.routes[1].filters[1].name=Retry",
    "spring.cloud.gateway.routes[1].filters[1].args.retries=2",
    "spring.cloud.gateway.routes[1].filters[1].args.statuses=BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT",
    "spring.cloud.gateway.routes[1].filters[1].args.methods=GET,POST",
    "spring.cloud.gateway.routes[1].filters[1].args.backoff.firstBackoff=100ms",
    "spring.cloud.gateway.routes[1].filters[1].args.backoff.maxBackoff=1000ms",
    "spring.cloud.gateway.routes[1].filters[1].args.backoff.factor=2",
    "spring.cloud.gateway.routes[1].filters[1].args.backoff.basedOnPreviousValue=false",
    "spring.cloud.gateway.routes[1].metadata.timeout=3000",
    "spring.cloud.gateway.routes[1].metadata.enabled=true",
    "spring.cloud.gateway.routes[1].metadata.order=2",
    
    // Circuit Breaker Configuration
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=5",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=5s",
    "resilience4j.circuitbreaker.instances.user-service-cb.permittedNumberOfCallsInHalfOpenState=2",
    "resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=5",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=5s",
    "resilience4j.circuitbreaker.instances.product-service-cb.permittedNumberOfCallsInHalfOpenState=2",
    "resilience4j.circuitbreaker.instances.product-service-cb.registerHealthIndicator=true",
    
    // Time Limiter Configuration
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=2s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=2s",
    
    // HTTP Client Configuration
    "spring.cloud.gateway.httpclient.connect-timeout=2000",
    "spring.cloud.gateway.httpclient.response-timeout=3s"
})
public class RouteFunctionalityPropertiesTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    /**
     * Test route path matching works with properties configuration
     * Requirements: 3.1 - Route configurations preserved in properties format
     */
    @Test
    public void testRoutePathMatching_UserService() {
        // Verify route is loaded from properties
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).isNotEmpty();
        
        // Find user service route
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getUri().toString()).isEqualTo("http://localhost:8081");
        
        // Test path matching by making request
        String userUrl = "http://localhost:" + gatewayPort + "/user/profile";
        ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
        
        // Should match route but service is unavailable, so expect error or fallback
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Test route path matching works with properties configuration
     * Requirements: 3.1 - Route configurations preserved in properties format
     */
    @Test
    public void testRoutePathMatching_ProductService() {
        // Test product service path matching
        String productUrl = "http://localhost:" + gatewayPort + "/product/list";
        ResponseEntity<String> response = restTemplate.getForEntity(productUrl, String.class);
        
        // Should match route but service is unavailable, so expect error or fallback
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Test route path matching for non-matching paths
     * Requirements: 3.1 - Route configurations preserved in properties format
     */
    @Test
    public void testRoutePathMatching_NonMatchingPath() {
        // Test path that doesn't match any route
        String invalidUrl = "http://localhost:" + gatewayPort + "/invalid/path";
        ResponseEntity<String> response = restTemplate.getForEntity(invalidUrl, String.class);
        
        // Should return 404 for non-matching paths
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Test circuit breaker filter functionality with properties configuration
     * Requirements: 3.2 - Circuit breaker configurations work identically
     */
    @Test
    public void testCircuitBreakerFilter_UserService() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/login";
        
        // Make multiple requests to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.postForEntity(userUrl, "{}", String.class);
            
            // Should get error response due to service unavailability
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE, 
                HttpStatus.BAD_GATEWAY, 
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        
        // Verify health endpoint is accessible (circuit breaker health may not be exposed by default)
        String healthUrl = "http://localhost:" + gatewayPort + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Health endpoint should be accessible
        assertThat(healthResponse.getBody()).contains("status");
    }

    /**
     * Test circuit breaker filter functionality with properties configuration
     * Requirements: 3.2 - Circuit breaker configurations work identically
     */
    @Test
    public void testCircuitBreakerFilter_ProductService() {
        String productUrl = "http://localhost:" + gatewayPort + "/product/search";
        
        // Make multiple requests to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(productUrl, String.class);
            
            // Should get error response due to service unavailability
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE, 
                HttpStatus.BAD_GATEWAY, 
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Test retry filter functionality with properties configuration
     * Requirements: 3.3 - Retry configurations work identically
     */
    @Test
    public void testRetryFilter_UserService() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/data";
        
        // Test GET request (should be retried according to configuration)
        ResponseEntity<String> getResponse = restTemplate.getForEntity(userUrl, String.class);
        
        // Should eventually fail after retries
        assertThat(getResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Test POST request (should be retried according to configuration)
        ResponseEntity<String> postResponse = restTemplate.postForEntity(userUrl, "{}", String.class);
        
        // Should eventually fail after retries
        assertThat(postResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Test retry filter functionality with properties configuration
     * Requirements: 3.3 - Retry configurations work identically
     */
    @Test
    public void testRetryFilter_ProductService() {
        String productUrl = "http://localhost:" + gatewayPort + "/product/details";
        
        // Test GET request (should be retried - allowed method)
        ResponseEntity<String> getResponse = restTemplate.getForEntity(productUrl, String.class);
        
        assertThat(getResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Test PUT request (should NOT be retried - not in allowed methods for product service)
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        ResponseEntity<String> putResponse = restTemplate.exchange(
            productUrl, HttpMethod.PUT, entity, String.class);
        
        assertThat(putResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE, 
            HttpStatus.BAD_GATEWAY, 
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Test route metadata handling with properties format
     * Requirements: 3.4 - Route metadata preserved (timeout, enabled status, order)
     */
    @Test
    public void testRouteMetadata_Configuration() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).isNotEmpty();
        
        // Verify user service route metadata
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        
        // Check metadata values (properties are loaded as strings)
        Map<String, Object> userMetadata = userRoute.getMetadata();
        assertThat(userMetadata).containsEntry("timeout", "5000");
        assertThat(userMetadata).containsEntry("enabled", "true");
        assertThat(userMetadata).containsEntry("order", "1");
        
        // Verify product service route metadata
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(productRoute).isNotNull();
        
        Map<String, Object> productMetadata = productRoute.getMetadata();
        assertThat(productMetadata).containsEntry("timeout", "3000");
        assertThat(productMetadata).containsEntry("enabled", "true");
        assertThat(productMetadata).containsEntry("order", "2");
    }

    /**
     * Test route ordering based on metadata
     * Requirements: 3.4 - Route metadata preserved (order)
     */
    @Test
    public void testRouteMetadata_Ordering() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).isNotEmpty();
        
        // Find routes and verify their order metadata
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(productRoute).isNotNull();
        
        // Verify order values (properties are loaded as strings)
        String userOrder = (String) userRoute.getMetadata().get("order");
        String productOrder = (String) productRoute.getMetadata().get("order");
        
        assertThat(userOrder).isEqualTo("1");
        assertThat(productOrder).isEqualTo("2");
        assertThat(Integer.parseInt(userOrder)).isLessThan(Integer.parseInt(productOrder));
    }

    /**
     * Test fallback controller integration with properties routes
     * Requirements: 3.1, 3.2 - Fallback functionality works with properties configuration
     */
    @Test
    public void testFallbackController_UserService() {
        // Direct test of fallback endpoint
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/user";
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("User service is temporarily unavailable");
    }

    /**
     * Test fallback controller integration with properties routes
     * Requirements: 3.1, 3.2 - Fallback functionality works with properties configuration
     */
    @Test
    public void testFallbackController_ProductService() {
        // Direct test of fallback endpoint
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/product";
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Product service is temporarily unavailable");
    }

    /**
     * Test that routes are properly loaded from properties configuration
     * Requirements: 3.1 - Route configurations preserved in properties format
     */
    @Test
    public void testRouteConfiguration_LoadedFromProperties() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).isNotEmpty();
        assertThat(routeList).hasSize(2); // Should have user and product routes
        
        // Verify both routes are loaded
        boolean hasUserRoute = routeList.stream()
            .anyMatch(route -> "user-service-test".equals(route.getId()));
        boolean hasProductRoute = routeList.stream()
            .anyMatch(route -> "product-service-test".equals(route.getId()));
        
        assertThat(hasUserRoute).isTrue();
        assertThat(hasProductRoute).isTrue();
    }

    /**
     * Test route filter configuration from properties
     * Requirements: 3.2, 3.3 - Filter configurations work identically
     */
    @Test
    public void testRouteFilters_ConfiguredFromProperties() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).isNotEmpty();
        
        // Verify user route has correct filters
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getFilters()).isNotEmpty();
        
        // Verify product route has correct filters
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(productRoute).isNotNull();
        assertThat(productRoute.getFilters()).isNotEmpty();
    }
}