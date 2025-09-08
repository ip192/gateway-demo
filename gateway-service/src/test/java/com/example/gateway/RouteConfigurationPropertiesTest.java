package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to validate route configuration parsing from properties files
 * Verifies that route definitions, predicates, filters, and metadata are correctly loaded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=test-route",
    "spring.cloud.gateway.routes[0].uri=http://localhost:9999",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**",
    "spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[0].filters[0].args.name=test-cb",
    "spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/test",
    "spring.cloud.gateway.routes[0].filters[1].name=Retry",
    "spring.cloud.gateway.routes[0].filters[1].args.retries=3",
    "spring.cloud.gateway.routes[0].metadata.timeout=5000",
    "spring.cloud.gateway.routes[0].metadata.enabled=true",
    "spring.cloud.gateway.routes[0].metadata.order=1"
})
class RouteConfigurationPropertiesTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    void testRouteDefinitionParsing() {
        assertThat(gatewayProperties.getRoutes()).isNotEmpty();
        
        RouteDefinition testRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "test-route".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(testRoute).isNotNull();
        assertThat(testRoute.getId()).isEqualTo("test-route");
        assertThat(testRoute.getUri().toString()).isEqualTo("http://localhost:9999");
    }

    @Test
    void testPredicateConfigurationParsing() {
        RouteDefinition testRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "test-route".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(testRoute).isNotNull();
        assertThat(testRoute.getPredicates()).hasSize(1);
        
        PredicateDefinition pathPredicate = testRoute.getPredicates().get(0);
        assertThat(pathPredicate.getName()).isEqualTo("Path");
        assertThat(pathPredicate.getArgs()).containsEntry("_genkey_0", "/test/**");
    }

    @Test
    void testFilterConfigurationParsing() {
        RouteDefinition testRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "test-route".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(testRoute).isNotNull();
        assertThat(testRoute.getFilters()).hasSize(2);
        
        // Test CircuitBreaker filter
        FilterDefinition circuitBreakerFilter = testRoute.getFilters().stream()
            .filter(filter -> "CircuitBreaker".equals(filter.getName()))
            .findFirst()
            .orElse(null);
        
        assertThat(circuitBreakerFilter).isNotNull();
        assertThat(circuitBreakerFilter.getArgs()).containsEntry("name", "test-cb");
        assertThat(circuitBreakerFilter.getArgs()).containsEntry("fallbackUri", "forward:/fallback/test");
        
        // Test Retry filter
        FilterDefinition retryFilter = testRoute.getFilters().stream()
            .filter(filter -> "Retry".equals(filter.getName()))
            .findFirst()
            .orElse(null);
        
        assertThat(retryFilter).isNotNull();
        assertThat(retryFilter.getArgs()).containsEntry("retries", "3");
    }

    @Test
    void testRouteMetadataParsing() {
        RouteDefinition testRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "test-route".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(testRoute).isNotNull();
        
        Map<String, Object> metadata = testRoute.getMetadata();
        assertThat(metadata).isNotEmpty();
        assertThat(metadata).containsEntry("timeout", "5000");
        assertThat(metadata).containsEntry("enabled", "true");
        assertThat(metadata).containsEntry("order", "1");
    }

    @Test
    void testComplexFilterArgumentsParsing() {
        // Test that complex filter arguments with nested properties are parsed correctly
        RouteDefinition testRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "test-route".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(testRoute).isNotNull();
        
        FilterDefinition retryFilter = testRoute.getFilters().stream()
            .filter(filter -> "Retry".equals(filter.getName()))
            .findFirst()
            .orElse(null);
        
        assertThat(retryFilter).isNotNull();
        assertThat(retryFilter.getArgs()).containsKey("retries");
        assertThat(retryFilter.getArgs().get("retries")).isEqualTo("3");
    }
}