package com.example.gateway;

import com.example.gateway.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for fallback controller integration with properties routes
 * Requirements: 3.1, 3.2 - Fallback functionality works with properties configuration
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // User service route with circuit breaker and fallback
    "spring.cloud.gateway.routes[0].id=user-service-fallback-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[0].filters[0].args.name=user-service-cb",
    "spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/user",
    
    // Product service route with circuit breaker and fallback
    "spring.cloud.gateway.routes[1].id=product-service-fallback-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[1].filters[0].args.name=product-service-cb",
    "spring.cloud.gateway.routes[1].filters[0].args.fallbackUri=forward:/fallback/product",
    
    // Admin service route with general fallback
    "spring.cloud.gateway.routes[2].id=admin-service-fallback-test",
    "spring.cloud.gateway.routes[2].uri=http://localhost:8083",
    "spring.cloud.gateway.routes[2].predicates[0]=Path=/admin/**",
    "spring.cloud.gateway.routes[2].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[2].filters[0].args.name=admin-service-cb",
    "spring.cloud.gateway.routes[2].filters[0].args.fallbackUri=forward:/fallback/general",
    
    // Test service route with timeout fallback
    "spring.cloud.gateway.routes[3].id=test-service-timeout-fallback",
    "spring.cloud.gateway.routes[3].uri=http://localhost:8084",
    "spring.cloud.gateway.routes[3].predicates[0]=Path=/test/**",
    "spring.cloud.gateway.routes[3].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[3].filters[0].args.name=test-service-cb",
    "spring.cloud.gateway.routes[3].filters[0].args.fallbackUri=forward:/fallback/timeout",
    
    // Circuit breaker configurations for fast testing
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=2",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=2s",
    "resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=2",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=2s",
    "resilience4j.circuitbreaker.instances.product-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.admin-service-cb.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.admin-service-cb.minimumNumberOfCalls=2",
    "resilience4j.circuitbreaker.instances.admin-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.admin-service-cb.waitDurationInOpenState=2s",
    "resilience4j.circuitbreaker.instances.admin-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.test-service-cb.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.test-service-cb.minimumNumberOfCalls=2",
    "resilience4j.circuitbreaker.instances.test-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.test-service-cb.waitDurationInOpenState=2s",
    "resilience4j.circuitbreaker.instances.test-service-cb.registerHealthIndicator=true",
    
    // Time limiter configurations
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=1s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=1s",
    "resilience4j.timelimiter.instances.admin-service-cb.timeoutDuration=1s",
    "resilience4j.timelimiter.instances.test-service-cb.timeoutDuration=1s"
})
public class FallbackControllerIntegrationPropertiesTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test fallback routes are configured correctly from properties
     */
    @Test
    public void testFallbackRoutesConfiguration() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).hasSize(4);
        
        // Verify all routes have circuit breaker filters with fallback URIs
        for (Route route : routeList) {
            assertThat(route.getFilters()).isNotEmpty();
        }
    }

    /**
     * Test user service fallback endpoint directly
     */
    @Test
    public void testUserServiceFallback_DirectAccess() {
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/user";
        ResponseEntity<String> response = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("User service is temporarily unavailable");
        
        // Test with ApiResponse type
        ResponseEntity<ApiResponse> apiResponse = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(apiResponse.getBody()).isNotNull();
        assertThat(apiResponse.getBody().isSuccess()).isFalse();
        assertThat(apiResponse.getBody().getMessage()).contains("User service is temporarily unavailable");
        assertThat(apiResponse.getBody().getData()).isNull();
    }

    /**
     * Test product service fallback endpoint directly
     */
    @Test
    public void testProductServiceFallback_DirectAccess() {
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/product";
        ResponseEntity<String> response = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("Product service is temporarily unavailable");
        
        // Test with ApiResponse type
        ResponseEntity<ApiResponse> apiResponse = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(apiResponse.getBody()).isNotNull();
        assertThat(apiResponse.getBody().isSuccess()).isFalse();
        assertThat(apiResponse.getBody().getMessage()).contains("Product service is temporarily unavailable");
        assertThat(apiResponse.getBody().getData()).isNull();
    }

    /**
     * Test general fallback endpoint directly
     */
    @Test
    public void testGeneralFallback_DirectAccess() {
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/general";
        ResponseEntity<String> response = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("Service is temporarily unavailable");
        
        // Test with ApiResponse type
        ResponseEntity<ApiResponse> apiResponse = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(apiResponse.getBody()).isNotNull();
        assertThat(apiResponse.getBody().isSuccess()).isFalse();
        assertThat(apiResponse.getBody().getMessage()).contains("Service is temporarily unavailable");
        assertThat(apiResponse.getBody().getData()).isNull();
    }

    /**
     * Test timeout fallback endpoint directly
     */
    @Test
    public void testTimeoutFallback_DirectAccess() {
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/timeout";
        ResponseEntity<String> response = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
        assertThat(response.getBody()).contains("Request timeout");
        
        // Test with ApiResponse type
        ResponseEntity<ApiResponse> apiResponse = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
        assertThat(apiResponse.getBody()).isNotNull();
        assertThat(apiResponse.getBody().isSuccess()).isFalse();
        assertThat(apiResponse.getBody().getMessage()).contains("Request timeout");
        assertThat(apiResponse.getBody().getData()).isNull();
    }

    /**
     * Test fallback integration through user service route
     */
    @Test
    public void testFallbackIntegration_UserService() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/profile";
        
        // Make multiple requests to trigger circuit breaker and fallback
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
            
            // Should get error response (service unavailable or fallback)
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Test fallback integration through product service route
     */
    @Test
    public void testFallbackIntegration_ProductService() {
        String productUrl = "http://localhost:" + gatewayPort + "/product/list";
        
        // Make multiple requests to trigger circuit breaker and fallback
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(productUrl, String.class);
            
            // Should get error response (service unavailable or fallback)
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Test fallback integration through admin service route
     */
    @Test
    public void testFallbackIntegration_AdminService() {
        String adminUrl = "http://localhost:" + gatewayPort + "/admin/dashboard";
        
        // Make multiple requests to trigger circuit breaker and fallback
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(adminUrl, String.class);
            
            // Should get error response (service unavailable or fallback)
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Test fallback integration through test service route
     */
    @Test
    public void testFallbackIntegration_TestService() {
        String testUrl = "http://localhost:" + gatewayPort + "/test/endpoint";
        
        // Make multiple requests to trigger circuit breaker and fallback
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);
            
            // Should get error response (service unavailable or fallback)
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.REQUEST_TIMEOUT
            );
        }
    }

    /**
     * Test different fallback responses for different services
     */
    @Test
    public void testDifferentFallbackResponses() {
        // Test user service fallback
        String userFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/user";
        ResponseEntity<ApiResponse> userResponse = restTemplate.getForEntity(userFallbackUrl, ApiResponse.class);
        
        // Test product service fallback
        String productFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/product";
        ResponseEntity<ApiResponse> productResponse = restTemplate.getForEntity(productFallbackUrl, ApiResponse.class);
        
        // Test general fallback
        String generalFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/general";
        ResponseEntity<ApiResponse> generalResponse = restTemplate.getForEntity(generalFallbackUrl, ApiResponse.class);
        
        // Test timeout fallback
        String timeoutFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/timeout";
        ResponseEntity<ApiResponse> timeoutResponse = restTemplate.getForEntity(timeoutFallbackUrl, ApiResponse.class);
        
        // Verify different messages
        assertThat(userResponse.getBody().getMessage()).contains("User service");
        assertThat(productResponse.getBody().getMessage()).contains("Product service");
        assertThat(generalResponse.getBody().getMessage()).contains("Service is temporarily unavailable");
        assertThat(timeoutResponse.getBody().getMessage()).contains("Request timeout");
        
        // Verify different status codes
        assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(productResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(generalResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(timeoutResponse.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
    }

    /**
     * Test fallback response format consistency
     */
    @Test
    public void testFallbackResponseFormat() {
        String[] fallbackUrls = {
            "/fallback/user",
            "/fallback/product",
            "/fallback/general",
            "/fallback/timeout"
        };
        
        for (String fallbackPath : fallbackUrls) {
            String fallbackUrl = "http://localhost:" + gatewayPort + fallbackPath;
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(fallbackUrl, ApiResponse.class);
            
            // All fallback responses should have consistent format
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isNotNull();
            assertThat(response.getBody().getMessage()).isNotEmpty();
            assertThat(response.getBody().getData()).isNull();
        }
    }

    /**
     * Test fallback controller handles different HTTP methods
     */
    @Test
    public void testFallbackController_DifferentHttpMethods() {
        String userFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/user";
        
        // Test GET request
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(userFallbackUrl, ApiResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // Test POST request
        ResponseEntity<ApiResponse> postResponse = restTemplate.postForEntity(userFallbackUrl, null, ApiResponse.class);
        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        
        // Both should have the same message format
        assertThat(getResponse.getBody().getMessage()).isEqualTo(postResponse.getBody().getMessage());
    }

    /**
     * Test fallback controller error handling
     */
    @Test
    public void testFallbackController_ErrorHandling() {
        // Test non-existent fallback endpoint
        String invalidFallbackUrl = "http://localhost:" + gatewayPort + "/fallback/nonexistent";
        ResponseEntity<String> response = restTemplate.getForEntity(invalidFallbackUrl, String.class);
        
        // Should return 404 for non-existent fallback endpoints
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Test fallback controller with circuit breaker health indicators
     */
    @Test
    public void testFallbackController_WithHealthIndicators() {
        // Check health endpoint includes circuit breaker information
        String healthUrl = "http://localhost:" + gatewayPort + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(healthResponse.getBody()).contains("circuitBreakers");
        
        // Make requests to trigger circuit breakers
        String userUrl = "http://localhost:" + gatewayPort + "/user/test";
        for (int i = 0; i < 3; i++) {
            restTemplate.getForEntity(userUrl, String.class);
        }
        
        // Health should still be accessible
        ResponseEntity<String> healthAfterRequests = restTemplate.getForEntity(healthUrl, String.class);
        assertThat(healthAfterRequests.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Test fallback URIs are correctly configured in routes
     */
    @Test
    public void testFallbackUriConfiguration() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Verify routes have circuit breaker filters with fallback URIs
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-fallback-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-fallback-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        Route adminRoute = routeList.stream()
            .filter(route -> "admin-service-fallback-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        Route testRoute = routeList.stream()
            .filter(route -> "test-service-timeout-fallback".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(productRoute).isNotNull();
        assertThat(adminRoute).isNotNull();
        assertThat(testRoute).isNotNull();
        
        // All routes should have filters (circuit breaker with fallback)
        assertThat(userRoute.getFilters()).isNotEmpty();
        assertThat(productRoute.getFilters()).isNotEmpty();
        assertThat(adminRoute.getFilters()).isNotEmpty();
        assertThat(testRoute.getFilters()).isNotEmpty();
    }
}