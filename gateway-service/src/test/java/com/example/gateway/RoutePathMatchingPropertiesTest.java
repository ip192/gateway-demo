package com.example.gateway;

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
 * Specific tests for route path matching functionality with properties configuration
 * Requirements: 3.1 - Route configurations preserved in properties format
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Multiple path patterns for comprehensive testing
    "spring.cloud.gateway.routes[0].id=user-exact-path",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/profile",
    
    "spring.cloud.gateway.routes[1].id=user-wildcard-path",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/user/**",
    
    "spring.cloud.gateway.routes[2].id=product-multiple-paths",
    "spring.cloud.gateway.routes[2].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[2].predicates[0]=Path=/product/**,/products/**",
    
    "spring.cloud.gateway.routes[3].id=api-versioned-path",
    "spring.cloud.gateway.routes[3].uri=http://localhost:8083",
    "spring.cloud.gateway.routes[3].predicates[0]=Path=/api/v{version}/**",
    
    "spring.cloud.gateway.routes[4].id=admin-complex-path",
    "spring.cloud.gateway.routes[4].uri=http://localhost:8084",
    "spring.cloud.gateway.routes[4].predicates[0]=Path=/admin/{segment}/management/**",
    "spring.cloud.gateway.routes[4].metadata.order=10"
})
public class RoutePathMatchingPropertiesTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    /**
     * Test exact path matching from properties configuration
     */
    @Test
    public void testExactPathMatching() {
        // Test exact path match
        String exactUrl = "http://localhost:" + gatewayPort + "/user/profile";
        ResponseEntity<String> response = restTemplate.getForEntity(exactUrl, String.class);
        
        // Should match route (service unavailable but route matched)
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        
        // Test path that doesn't match exactly
        String nonExactUrl = "http://localhost:" + gatewayPort + "/user/profile/extra";
        ResponseEntity<String> nonExactResponse = restTemplate.getForEntity(nonExactUrl, String.class);
        
        // Should still match due to wildcard route
        assertThat(nonExactResponse.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Test wildcard path matching from properties configuration
     */
    @Test
    public void testWildcardPathMatching() {
        // Test various paths under /user/**
        String[] userPaths = {
            "/user/login",
            "/user/profile/settings",
            "/user/data/export",
            "/user/admin/dashboard"
        };
        
        for (String path : userPaths) {
            String url = "http://localhost:" + gatewayPort + path;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // Should match wildcard route
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Test multiple path patterns in single route from properties configuration
     */
    @Test
    public void testMultiplePathPatterns() {
        // Test first pattern /product/**
        String productUrl = "http://localhost:" + gatewayPort + "/product/list";
        ResponseEntity<String> productResponse = restTemplate.getForEntity(productUrl, String.class);
        assertThat(productResponse.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        
        // Test second pattern /products/**
        String productsUrl = "http://localhost:" + gatewayPort + "/products/search";
        ResponseEntity<String> productsResponse = restTemplate.getForEntity(productsUrl, String.class);
        assertThat(productsResponse.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * Test path variables in route patterns from properties configuration
     */
    @Test
    public void testPathVariables() {
        // Test versioned API paths
        String[] versionedPaths = {
            "/api/v1/users",
            "/api/v2/products",
            "/api/v3/orders/123"
        };
        
        for (String path : versionedPaths) {
            String url = "http://localhost:" + gatewayPort + path;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // Should match versioned route pattern
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Test complex path patterns with multiple variables from properties configuration
     */
    @Test
    public void testComplexPathPatterns() {
        // Test admin paths with segments
        String[] adminPaths = {
            "/admin/users/management/list",
            "/admin/products/management/create",
            "/admin/orders/management/update/123"
        };
        
        for (String path : adminPaths) {
            String url = "http://localhost:" + gatewayPort + path;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // Should match complex pattern
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Test non-matching paths return 404
     */
    @Test
    public void testNonMatchingPaths() {
        String[] nonMatchingPaths = {
            "/invalid/path",
            "/unknown/route",
            "/api/invalid",
            "/admin/invalid"
        };
        
        for (String path : nonMatchingPaths) {
            String url = "http://localhost:" + gatewayPort + path;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            // Should return 404 for non-matching paths
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Test route configuration is loaded correctly from properties
     */
    @Test
    public void testRouteConfigurationLoading() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).hasSize(5); // Should have all 5 configured routes
        
        // Verify specific routes exist
        boolean hasUserExactRoute = routeList.stream()
            .anyMatch(route -> "user-exact-path".equals(route.getId()));
        boolean hasUserWildcardRoute = routeList.stream()
            .anyMatch(route -> "user-wildcard-path".equals(route.getId()));
        boolean hasProductMultipleRoute = routeList.stream()
            .anyMatch(route -> "product-multiple-paths".equals(route.getId()));
        boolean hasVersionedRoute = routeList.stream()
            .anyMatch(route -> "api-versioned-path".equals(route.getId()));
        boolean hasComplexRoute = routeList.stream()
            .anyMatch(route -> "admin-complex-path".equals(route.getId()));
        
        assertThat(hasUserExactRoute).isTrue();
        assertThat(hasUserWildcardRoute).isTrue();
        assertThat(hasProductMultipleRoute).isTrue();
        assertThat(hasVersionedRoute).isTrue();
        assertThat(hasComplexRoute).isTrue();
    }

    /**
     * Test route priority and ordering affects path matching
     */
    @Test
    public void testRouteOrdering() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Find admin route with specific order
        Route adminRoute = routeList.stream()
            .filter(route -> "admin-complex-path".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(adminRoute).isNotNull();
        assertThat(adminRoute.getMetadata()).containsEntry("order", 10);
    }

    /**
     * Test case sensitivity in path matching
     */
    @Test
    public void testPathCaseSensitivity() {
        // Test lowercase path
        String lowerUrl = "http://localhost:" + gatewayPort + "/user/profile";
        ResponseEntity<String> lowerResponse = restTemplate.getForEntity(lowerUrl, String.class);
        assertThat(lowerResponse.getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
        
        // Test uppercase path (should not match)
        String upperUrl = "http://localhost:" + gatewayPort + "/USER/PROFILE";
        ResponseEntity<String> upperResponse = restTemplate.getForEntity(upperUrl, String.class);
        assertThat(upperResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}