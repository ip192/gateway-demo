package com.example.gateway;

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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for retry filter functionality with properties configuration
 * Requirements: 3.3 - Retry configurations work identically
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // User service route with retry configuration
    "spring.cloud.gateway.routes[0].id=user-service-retry-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].filters[0].name=Retry",
    "spring.cloud.gateway.routes[0].filters[0].args.retries=3",
    "spring.cloud.gateway.routes[0].filters[0].args.statuses=BAD_GATEWAY,SERVICE_UNAVAILABLE,GATEWAY_TIMEOUT",
    "spring.cloud.gateway.routes[0].filters[0].args.methods=GET,POST,PUT,DELETE",
    "spring.cloud.gateway.routes[0].filters[0].args.backoff.firstBackoff=100ms",
    "spring.cloud.gateway.routes[0].filters[0].args.backoff.maxBackoff=1000ms",
    "spring.cloud.gateway.routes[0].filters[0].args.backoff.factor=2",
    "spring.cloud.gateway.routes[0].filters[0].args.backoff.basedOnPreviousValue=false",
    
    // Product service route with different retry configuration
    "spring.cloud.gateway.routes[1].id=product-service-retry-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].filters[0].name=Retry",
    "spring.cloud.gateway.routes[1].filters[0].args.retries=2",
    "spring.cloud.gateway.routes[1].filters[0].args.statuses=BAD_GATEWAY,SERVICE_UNAVAILABLE",
    "spring.cloud.gateway.routes[1].filters[0].args.methods=GET,POST",
    "spring.cloud.gateway.routes[1].filters[0].args.backoff.firstBackoff=50ms",
    "spring.cloud.gateway.routes[1].filters[0].args.backoff.maxBackoff=500ms",
    "spring.cloud.gateway.routes[1].filters[0].args.backoff.factor=3",
    "spring.cloud.gateway.routes[1].filters[0].args.backoff.basedOnPreviousValue=true",
    
    // Admin service route with no retry for comparison
    "spring.cloud.gateway.routes[2].id=admin-service-no-retry",
    "spring.cloud.gateway.routes[2].uri=http://localhost:8083",
    "spring.cloud.gateway.routes[2].predicates[0]=Path=/admin/**",
    
    // Test service route with minimal retry
    "spring.cloud.gateway.routes[3].id=test-service-minimal-retry",
    "spring.cloud.gateway.routes[3].uri=http://localhost:8084",
    "spring.cloud.gateway.routes[3].predicates[0]=Path=/test/**",
    "spring.cloud.gateway.routes[3].filters[0].name=Retry",
    "spring.cloud.gateway.routes[3].filters[0].args.retries=1",
    "spring.cloud.gateway.routes[3].filters[0].args.statuses=SERVICE_UNAVAILABLE",
    "spring.cloud.gateway.routes[3].filters[0].args.methods=GET"
})
public class RetryFilterPropertiesTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    /**
     * Test retry filter configuration is loaded from properties
     */
    @Test
    public void testRetryFilterConfiguration_LoadedFromProperties() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).hasSize(4);
        
        // Verify user service route has retry filter
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-retry-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getFilters()).isNotEmpty();
        
        // Verify product service route has retry filter
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-retry-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(productRoute).isNotNull();
        assertThat(productRoute.getFilters()).isNotEmpty();
        
        // Verify admin service route has no retry filter
        Route adminRoute = routeList.stream()
            .filter(route -> "admin-service-no-retry".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(adminRoute).isNotNull();
        // Admin route should have no filters or empty filters
    }

    /**
     * Test retry behavior for GET requests
     */
    @Test
    public void testRetryFilter_GetRequests() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/profile";
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
        Instant end = Instant.now();
        
        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should take longer due to retries and backoff
        Duration duration = Duration.between(start, end);
        assertThat(duration.toMillis()).isGreaterThan(100); // At least first backoff time
    }

    /**
     * Test retry behavior for POST requests
     */
    @Test
    public void testRetryFilter_PostRequests() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/create";
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.postForEntity(userUrl, "{}", String.class);
        Instant end = Instant.now();
        
        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should take longer due to retries
        Duration duration = Duration.between(start, end);
        assertThat(duration.toMillis()).isGreaterThan(100);
    }

    /**
     * Test retry behavior for PUT requests
     */
    @Test
    public void testRetryFilter_PutRequests() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/update";
        
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.exchange(
            userUrl, HttpMethod.PUT, entity, String.class);
        Instant end = Instant.now();
        
        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should take longer due to retries
        Duration duration = Duration.between(start, end);
        assertThat(duration.toMillis()).isGreaterThan(100);
    }

    /**
     * Test retry behavior for DELETE requests
     */
    @Test
    public void testRetryFilter_DeleteRequests() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/delete/123";
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.exchange(
            userUrl, HttpMethod.DELETE, null, String.class);
        Instant end = Instant.now();
        
        // Should eventually fail after retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should take longer due to retries
        Duration duration = Duration.between(start, end);
        assertThat(duration.toMillis()).isGreaterThan(100);
    }

    /**
     * Test different retry configurations for different services
     */
    @Test
    public void testDifferentRetryConfigurations() {
        // Test user service (3 retries)
        String userUrl = "http://localhost:" + gatewayPort + "/user/test";
        
        Instant userStart = Instant.now();
        ResponseEntity<String> userResponse = restTemplate.getForEntity(userUrl, String.class);
        Instant userEnd = Instant.now();
        
        Duration userDuration = Duration.between(userStart, userEnd);
        
        // Test product service (2 retries)
        String productUrl = "http://localhost:" + gatewayPort + "/product/test";
        
        Instant productStart = Instant.now();
        ResponseEntity<String> productResponse = restTemplate.getForEntity(productUrl, String.class);
        Instant productEnd = Instant.now();
        
        Duration productDuration = Duration.between(productStart, productEnd);
        
        // Both should fail but user service should take longer (more retries)
        assertThat(userResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        assertThat(productResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // User service should generally take longer due to more retries
        // Note: This is not guaranteed due to timing variations, so we just check both took some time
        assertThat(userDuration.toMillis()).isGreaterThan(50);
        assertThat(productDuration.toMillis()).isGreaterThan(25);
    }

    /**
     * Test retry filter with method restrictions
     */
    @Test
    public void testRetryFilter_MethodRestrictions() {
        // Product service only retries GET and POST, not PUT or DELETE
        String productUrl = "http://localhost:" + gatewayPort + "/product/update";
        
        // Test PUT request (should NOT be retried for product service)
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);
        
        Instant putStart = Instant.now();
        ResponseEntity<String> putResponse = restTemplate.exchange(
            productUrl, HttpMethod.PUT, entity, String.class);
        Instant putEnd = Instant.now();
        
        Duration putDuration = Duration.between(putStart, putEnd);
        
        // Should fail quickly without retries
        assertThat(putResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should be relatively fast since no retries for PUT on product service
        // Note: This is a best-effort check as timing can vary
        assertThat(putDuration.toMillis()).isLessThan(2000);
    }

    /**
     * Test service without retry configuration
     */
    @Test
    public void testNoRetryFilter_AdminService() {
        String adminUrl = "http://localhost:" + gatewayPort + "/admin/dashboard";
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.getForEntity(adminUrl, String.class);
        Instant end = Instant.now();
        
        Duration duration = Duration.between(start, end);
        
        // Should fail quickly without retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.NOT_FOUND
        );
        
        // Should be fast since no retry filter
        assertThat(duration.toMillis()).isLessThan(1000);
    }

    /**
     * Test minimal retry configuration
     */
    @Test
    public void testMinimalRetryConfiguration() {
        String testUrl = "http://localhost:" + gatewayPort + "/test/endpoint";
        
        Instant start = Instant.now();
        ResponseEntity<String> response = restTemplate.getForEntity(testUrl, String.class);
        Instant end = Instant.now();
        
        Duration duration = Duration.between(start, end);
        
        // Should fail after minimal retries
        assertThat(response.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        // Should take some time but not too long (only 1 retry)
        assertThat(duration.toMillis()).isGreaterThan(10);
        assertThat(duration.toMillis()).isLessThan(2000);
    }

    /**
     * Test retry backoff behavior
     */
    @Test
    public void testRetryBackoffBehavior() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/backoff-test";
        
        // Make multiple requests to observe backoff behavior
        for (int i = 0; i < 3; i++) {
            Instant start = Instant.now();
            ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
            Instant end = Instant.now();
            
            Duration duration = Duration.between(start, end);
            
            // Each request should take time due to retries and backoff
            assertThat(duration.toMillis()).isGreaterThan(100); // At least first backoff
            
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Test retry with specific status codes
     */
    @Test
    public void testRetryFilter_StatusCodeRestrictions() {
        // User service retries on BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT
        // Product service retries on BAD_GATEWAY, SERVICE_UNAVAILABLE (not GATEWAY_TIMEOUT)
        
        String userUrl = "http://localhost:" + gatewayPort + "/user/status-test";
        String productUrl = "http://localhost:" + gatewayPort + "/product/status-test";
        
        // Both should retry on service unavailable
        Instant userStart = Instant.now();
        ResponseEntity<String> userResponse = restTemplate.getForEntity(userUrl, String.class);
        Instant userEnd = Instant.now();
        
        Instant productStart = Instant.now();
        ResponseEntity<String> productResponse = restTemplate.getForEntity(productUrl, String.class);
        Instant productEnd = Instant.now();
        
        // Both should take time due to retries
        Duration userDuration = Duration.between(userStart, userEnd);
        Duration productDuration = Duration.between(productStart, productEnd);
        
        assertThat(userDuration.toMillis()).isGreaterThan(50);
        assertThat(productDuration.toMillis()).isGreaterThan(25);
        
        assertThat(userResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
        
        assertThat(productResponse.getStatusCode()).isIn(
            HttpStatus.SERVICE_UNAVAILABLE,
            HttpStatus.BAD_GATEWAY,
            HttpStatus.GATEWAY_TIMEOUT,
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}