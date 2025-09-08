package com.example.gateway;

import com.example.gateway.config.DynamicRouteLocator;
import com.example.gateway.config.GatewayRoutingProperties;
import com.example.gateway.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DynamicRouteLocator with actual Spring Cloud Gateway components
 */
@SpringBootTest(classes = {GatewayApplication.class})
@TestPropertySource(properties = {
        "gateway.routes[0].id=user-service-test",
        "gateway.routes[0].uri=http://localhost:8081",
        "gateway.routes[0].predicates[0].name=Path",
        "gateway.routes[0].predicates[0].args.pattern=/user/**",
        "gateway.routes[0].metadata.enabled=true",
        "gateway.routes[0].metadata.order=1",
        "gateway.routes[1].id=product-service-test",
        "gateway.routes[1].uri=http://localhost:8082",
        "gateway.routes[1].predicates[0].name=Path",
        "gateway.routes[1].predicates[0].args.pattern=/product/**",
        "gateway.routes[1].metadata.enabled=true",
        "gateway.routes[1].metadata.order=2"
})
class DynamicRouteLocatorIntegrationTest {

    @Autowired
    private GatewayRoutingProperties routingProperties;
    
    @Autowired
    private RouteLocatorBuilder routeLocatorBuilder;

    @Test
    void testDynamicRouteLocatorCreation() {
        // Given
        DynamicRouteLocator dynamicRouteLocator = new DynamicRouteLocator(routingProperties, routeLocatorBuilder);

        // When
        Flux<Route> routes = dynamicRouteLocator.getRoutes();

        // Then - Should create routes based on configuration
        List<Route> routeList = routes.collectList().block();
        assertThat(routeList).hasSize(2);
        
        // Verify routes are created with correct IDs
        Set<String> routeIds = new HashSet<>();
        for (Route route : routeList) {
            routeIds.add(route.getId());
        }
        assertThat(routeIds).contains("user-service-test", "product-service-test");
    }

    @Test
    void testRouteConfigurationProperties() {
        // When
        List<RouteConfig> routes = routingProperties.getRoutes();

        // Then
        assertThat(routes).hasSize(2);
        
        RouteConfig userRoute = routes.stream()
                .filter(r -> "user-service-test".equals(r.getId()))
                .findFirst()
                .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getUri()).isEqualTo("http://localhost:8081");
        assertThat(userRoute.getMetadata().isEnabled()).isTrue();
        assertThat(userRoute.getMetadata().getOrder()).isEqualTo(1);
        assertThat(userRoute.getPredicates()).hasSize(1);
        assertThat(userRoute.getPredicates().get(0).getName()).isEqualTo("Path");
        assertThat(userRoute.getPredicates().get(0).getArgs().get("pattern")).isEqualTo("/user/**");
    }

    @Test
    void testDisabledRouteHandling() {
        // Given - Create a route config with disabled metadata
        GatewayRoutingProperties testProperties = new GatewayRoutingProperties();
        RouteConfig disabledRoute = createDisabledRouteConfig();
        testProperties.setRoutes(Arrays.asList(disabledRoute));
        
        DynamicRouteLocator dynamicRouteLocator = new DynamicRouteLocator(testProperties, routeLocatorBuilder);

        // When
        Flux<Route> routes = dynamicRouteLocator.getRoutes();

        // Then - No routes should be created for disabled routes
        assertThat(routes.collectList().block()).isEmpty();
    }

    @Test
    void testRouteValidation() {
        // Given
        DynamicRouteLocator dynamicRouteLocator = new DynamicRouteLocator(routingProperties, routeLocatorBuilder);
        
        // When
        List<RouteConfig> routes = routingProperties.getRoutes();
        
        // Then - All configured routes should be valid
        for (RouteConfig route : routes) {
            assertThat(dynamicRouteLocator.validateRouteConfig(route)).isTrue();
            assertThat(dynamicRouteLocator.isRouteEnabled(route)).isTrue();
            assertThat(dynamicRouteLocator.getRouteOrder(route)).isGreaterThanOrEqualTo(0);
        }
    }

    // Helper methods for creating test configurations

    private RouteConfig createDisabledRouteConfig() {
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setId("disabled-route");
        routeConfig.setUri("http://localhost:9999");
        
        RouteMetadata metadata = new RouteMetadata();
        metadata.setEnabled(false);
        metadata.setOrder(0);
        routeConfig.setMetadata(metadata);
        
        PredicateConfig pathPredicate = new PredicateConfig();
        pathPredicate.setName("Path");
        Map<String, String> args = new HashMap<>();
        args.put("pattern", "/disabled/**");
        pathPredicate.setArgs(args);
        routeConfig.setPredicates(Arrays.asList(pathPredicate));
        
        return routeConfig;
    }
}