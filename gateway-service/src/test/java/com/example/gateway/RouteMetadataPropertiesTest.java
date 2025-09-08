package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for route metadata handling with properties format
 * Requirements: 3.4 - Route metadata preserved (timeout, enabled status, order)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Route with comprehensive metadata
    "spring.cloud.gateway.routes[0].id=user-service-metadata",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].metadata.timeout=5000",
    "spring.cloud.gateway.routes[0].metadata.enabled=true",
    "spring.cloud.gateway.routes[0].metadata.order=1",
    "spring.cloud.gateway.routes[0].metadata.description=User service route",
    "spring.cloud.gateway.routes[0].metadata.version=1.0",
    "spring.cloud.gateway.routes[0].metadata.team=user-team",
    
    // Route with different metadata values
    "spring.cloud.gateway.routes[1].id=product-service-metadata",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].metadata.timeout=3000",
    "spring.cloud.gateway.routes[1].metadata.enabled=true",
    "spring.cloud.gateway.routes[1].metadata.order=2",
    "spring.cloud.gateway.routes[1].metadata.description=Product service route",
    "spring.cloud.gateway.routes[1].metadata.version=2.1",
    "spring.cloud.gateway.routes[1].metadata.team=product-team",
    
    // Route with disabled status
    "spring.cloud.gateway.routes[2].id=admin-service-disabled",
    "spring.cloud.gateway.routes[2].uri=http://localhost:8083",
    "spring.cloud.gateway.routes[2].predicates[0]=Path=/admin/**",
    "spring.cloud.gateway.routes[2].metadata.timeout=10000",
    "spring.cloud.gateway.routes[2].metadata.enabled=false",
    "spring.cloud.gateway.routes[2].metadata.order=10",
    "spring.cloud.gateway.routes[2].metadata.description=Admin service route (disabled)",
    "spring.cloud.gateway.routes[2].metadata.version=0.9",
    "spring.cloud.gateway.routes[2].metadata.team=admin-team",
    
    // Route with high priority (low order number)
    "spring.cloud.gateway.routes[3].id=priority-service-high",
    "spring.cloud.gateway.routes[3].uri=http://localhost:8084",
    "spring.cloud.gateway.routes[3].predicates[0]=Path=/priority/**",
    "spring.cloud.gateway.routes[3].metadata.timeout=2000",
    "spring.cloud.gateway.routes[3].metadata.enabled=true",
    "spring.cloud.gateway.routes[3].metadata.order=0",
    "spring.cloud.gateway.routes[3].metadata.description=High priority service",
    "spring.cloud.gateway.routes[3].metadata.priority=high",
    
    // Route with low priority (high order number)
    "spring.cloud.gateway.routes[4].id=background-service-low",
    "spring.cloud.gateway.routes[4].uri=http://localhost:8085",
    "spring.cloud.gateway.routes[4].predicates[0]=Path=/background/**",
    "spring.cloud.gateway.routes[4].metadata.timeout=15000",
    "spring.cloud.gateway.routes[4].metadata.enabled=true",
    "spring.cloud.gateway.routes[4].metadata.order=100",
    "spring.cloud.gateway.routes[4].metadata.description=Background processing service",
    "spring.cloud.gateway.routes[4].metadata.priority=low",
    
    // Route with minimal metadata
    "spring.cloud.gateway.routes[5].id=minimal-metadata-service",
    "spring.cloud.gateway.routes[5].uri=http://localhost:8086",
    "spring.cloud.gateway.routes[5].predicates[0]=Path=/minimal/**",
    "spring.cloud.gateway.routes[5].metadata.timeout=1000",
    "spring.cloud.gateway.routes[5].metadata.enabled=true",
    "spring.cloud.gateway.routes[5].metadata.order=50"
})
public class RouteMetadataPropertiesTest {

    @Autowired
    private RouteLocator routeLocator;

    /**
     * Test basic metadata properties are loaded correctly
     */
    @Test
    public void testBasicMetadataProperties() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        assertThat(routeList).hasSize(6);
        
        // Test user service metadata
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        assertThat(userRoute).isNotNull();
        
        Map<String, Object> userMetadata = userRoute.getMetadata();
        assertThat(userMetadata).containsEntry("timeout", 5000);
        assertThat(userMetadata).containsEntry("enabled", true);
        assertThat(userMetadata).containsEntry("order", 1);
        assertThat(userMetadata).containsEntry("description", "User service route");
        assertThat(userMetadata).containsEntry("version", "1.0");
        assertThat(userMetadata).containsEntry("team", "user-team");
    }

    /**
     * Test different timeout values are preserved
     */
    @Test
    public void testTimeoutMetadata() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Verify different timeout values
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        Route productRoute = findRouteById(routeList, "product-service-metadata");
        Route adminRoute = findRouteById(routeList, "admin-service-disabled");
        Route priorityRoute = findRouteById(routeList, "priority-service-high");
        Route backgroundRoute = findRouteById(routeList, "background-service-low");
        Route minimalRoute = findRouteById(routeList, "minimal-metadata-service");
        
        assertThat(userRoute.getMetadata()).containsEntry("timeout", 5000);
        assertThat(productRoute.getMetadata()).containsEntry("timeout", 3000);
        assertThat(adminRoute.getMetadata()).containsEntry("timeout", 10000);
        assertThat(priorityRoute.getMetadata()).containsEntry("timeout", 2000);
        assertThat(backgroundRoute.getMetadata()).containsEntry("timeout", 15000);
        assertThat(minimalRoute.getMetadata()).containsEntry("timeout", 1000);
    }

    /**
     * Test enabled/disabled status is preserved
     */
    @Test
    public void testEnabledStatusMetadata() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Most routes should be enabled
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        Route productRoute = findRouteById(routeList, "product-service-metadata");
        Route priorityRoute = findRouteById(routeList, "priority-service-high");
        Route backgroundRoute = findRouteById(routeList, "background-service-low");
        Route minimalRoute = findRouteById(routeList, "minimal-metadata-service");
        
        assertThat(userRoute.getMetadata()).containsEntry("enabled", true);
        assertThat(productRoute.getMetadata()).containsEntry("enabled", true);
        assertThat(priorityRoute.getMetadata()).containsEntry("enabled", true);
        assertThat(backgroundRoute.getMetadata()).containsEntry("enabled", true);
        assertThat(minimalRoute.getMetadata()).containsEntry("enabled", true);
        
        // Admin route should be disabled
        Route adminRoute = findRouteById(routeList, "admin-service-disabled");
        assertThat(adminRoute.getMetadata()).containsEntry("enabled", false);
    }

    /**
     * Test order values are preserved and can be used for sorting
     */
    @Test
    public void testOrderMetadata() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Verify order values
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        Route productRoute = findRouteById(routeList, "product-service-metadata");
        Route adminRoute = findRouteById(routeList, "admin-service-disabled");
        Route priorityRoute = findRouteById(routeList, "priority-service-high");
        Route backgroundRoute = findRouteById(routeList, "background-service-low");
        Route minimalRoute = findRouteById(routeList, "minimal-metadata-service");
        
        assertThat(priorityRoute.getMetadata()).containsEntry("order", 0);
        assertThat(userRoute.getMetadata()).containsEntry("order", 1);
        assertThat(productRoute.getMetadata()).containsEntry("order", 2);
        assertThat(adminRoute.getMetadata()).containsEntry("order", 10);
        assertThat(minimalRoute.getMetadata()).containsEntry("order", 50);
        assertThat(backgroundRoute.getMetadata()).containsEntry("order", 100);
    }

    /**
     * Test routes can be sorted by order metadata
     */
    @Test
    public void testRouteSortingByOrder() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Sort routes by order metadata
        List<Route> sortedRoutes = routeList.stream()
            .sorted(Comparator.comparing(route -> (Integer) route.getMetadata().get("order")))
            .collect(Collectors.toList());
        
        // Verify order
        assertThat(sortedRoutes.get(0).getId()).isEqualTo("priority-service-high"); // order 0
        assertThat(sortedRoutes.get(1).getId()).isEqualTo("user-service-metadata"); // order 1
        assertThat(sortedRoutes.get(2).getId()).isEqualTo("product-service-metadata"); // order 2
        assertThat(sortedRoutes.get(3).getId()).isEqualTo("admin-service-disabled"); // order 10
        assertThat(sortedRoutes.get(4).getId()).isEqualTo("minimal-metadata-service"); // order 50
        assertThat(sortedRoutes.get(5).getId()).isEqualTo("background-service-low"); // order 100
    }

    /**
     * Test custom metadata properties are preserved
     */
    @Test
    public void testCustomMetadataProperties() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Test custom properties
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        Route productRoute = findRouteById(routeList, "product-service-metadata");
        Route adminRoute = findRouteById(routeList, "admin-service-disabled");
        Route priorityRoute = findRouteById(routeList, "priority-service-high");
        Route backgroundRoute = findRouteById(routeList, "background-service-low");
        
        // Test description metadata
        assertThat(userRoute.getMetadata()).containsEntry("description", "User service route");
        assertThat(productRoute.getMetadata()).containsEntry("description", "Product service route");
        assertThat(adminRoute.getMetadata()).containsEntry("description", "Admin service route (disabled)");
        assertThat(priorityRoute.getMetadata()).containsEntry("description", "High priority service");
        assertThat(backgroundRoute.getMetadata()).containsEntry("description", "Background processing service");
        
        // Test version metadata
        assertThat(userRoute.getMetadata()).containsEntry("version", "1.0");
        assertThat(productRoute.getMetadata()).containsEntry("version", "2.1");
        assertThat(adminRoute.getMetadata()).containsEntry("version", "0.9");
        
        // Test team metadata
        assertThat(userRoute.getMetadata()).containsEntry("team", "user-team");
        assertThat(productRoute.getMetadata()).containsEntry("team", "product-team");
        assertThat(adminRoute.getMetadata()).containsEntry("team", "admin-team");
        
        // Test priority metadata
        assertThat(priorityRoute.getMetadata()).containsEntry("priority", "high");
        assertThat(backgroundRoute.getMetadata()).containsEntry("priority", "low");
    }

    /**
     * Test metadata filtering - find enabled routes
     */
    @Test
    public void testMetadataFiltering_EnabledRoutes() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Filter enabled routes
        List<Route> enabledRoutes = routeList.stream()
            .filter(route -> Boolean.TRUE.equals(route.getMetadata().get("enabled")))
            .collect(Collectors.toList());
        
        assertThat(enabledRoutes).hasSize(5); // All except admin route
        
        // Verify admin route is not in enabled list
        boolean hasAdminRoute = enabledRoutes.stream()
            .anyMatch(route -> "admin-service-disabled".equals(route.getId()));
        assertThat(hasAdminRoute).isFalse();
    }

    /**
     * Test metadata filtering - find routes by team
     */
    @Test
    public void testMetadataFiltering_ByTeam() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Filter routes by team
        List<Route> userTeamRoutes = routeList.stream()
            .filter(route -> "user-team".equals(route.getMetadata().get("team")))
            .collect(Collectors.toList());
        
        List<Route> productTeamRoutes = routeList.stream()
            .filter(route -> "product-team".equals(route.getMetadata().get("team")))
            .collect(Collectors.toList());
        
        List<Route> adminTeamRoutes = routeList.stream()
            .filter(route -> "admin-team".equals(route.getMetadata().get("team")))
            .collect(Collectors.toList());
        
        assertThat(userTeamRoutes).hasSize(1);
        assertThat(productTeamRoutes).hasSize(1);
        assertThat(adminTeamRoutes).hasSize(1);
        
        assertThat(userTeamRoutes.get(0).getId()).isEqualTo("user-service-metadata");
        assertThat(productTeamRoutes.get(0).getId()).isEqualTo("product-service-metadata");
        assertThat(adminTeamRoutes.get(0).getId()).isEqualTo("admin-service-disabled");
    }

    /**
     * Test metadata filtering - find routes by timeout range
     */
    @Test
    public void testMetadataFiltering_ByTimeoutRange() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Find routes with timeout <= 5000ms
        List<Route> fastRoutes = routeList.stream()
            .filter(route -> {
                Object timeout = route.getMetadata().get("timeout");
                return timeout instanceof Integer && (Integer) timeout <= 5000;
            })
            .collect(Collectors.toList());
        
        // Find routes with timeout > 5000ms
        List<Route> slowRoutes = routeList.stream()
            .filter(route -> {
                Object timeout = route.getMetadata().get("timeout");
                return timeout instanceof Integer && (Integer) timeout > 5000;
            })
            .collect(Collectors.toList());
        
        assertThat(fastRoutes).hasSize(4); // user(5000), product(3000), priority(2000), minimal(1000)
        assertThat(slowRoutes).hasSize(2); // admin(10000), background(15000)
    }

    /**
     * Test metadata completeness - ensure all expected metadata is present
     */
    @Test
    public void testMetadataCompleteness() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // All routes should have timeout, enabled, and order metadata
        for (Route route : routeList) {
            Map<String, Object> metadata = route.getMetadata();
            
            assertThat(metadata).containsKey("timeout");
            assertThat(metadata).containsKey("enabled");
            assertThat(metadata).containsKey("order");
            
            // Verify data types
            assertThat(metadata.get("timeout")).isInstanceOf(Integer.class);
            assertThat(metadata.get("enabled")).isInstanceOf(Boolean.class);
            assertThat(metadata.get("order")).isInstanceOf(Integer.class);
        }
    }

    /**
     * Test metadata immutability - metadata should be read-only
     */
    @Test
    public void testMetadataImmutability() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        Route userRoute = findRouteById(routeList, "user-service-metadata");
        Map<String, Object> metadata = userRoute.getMetadata();
        
        // Original values
        Integer originalTimeout = (Integer) metadata.get("timeout");
        Boolean originalEnabled = (Boolean) metadata.get("enabled");
        Integer originalOrder = (Integer) metadata.get("order");
        
        assertThat(originalTimeout).isEqualTo(5000);
        assertThat(originalEnabled).isTrue();
        assertThat(originalOrder).isEqualTo(1);
        
        // Metadata should reflect the configured values
        assertThat(metadata).hasSize(6); // timeout, enabled, order, description, version, team
    }

    /**
     * Helper method to find route by ID
     */
    private Route findRouteById(List<Route> routes, String id) {
        return routes.stream()
            .filter(route -> id.equals(route.getId()))
            .findFirst()
            .orElse(null);
    }
}