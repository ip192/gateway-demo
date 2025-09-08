package com.example.gateway;

import com.example.gateway.config.RouteConfigValidator;
import com.example.gateway.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteConfigValidatorTest {

    private RouteConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RouteConfigValidator();
    }

    @Test
    void testValidateValidRoute() {
        RouteConfig route = createValidRoute();
        
        assertDoesNotThrow(() -> validator.validateRoute(route));
    }

    @Test
    void testValidateNullRoute() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(null)
        );
        assertEquals("Route configuration cannot be null", exception.getMessage());
    }

    @Test
    void testValidateRouteWithNullId() {
        RouteConfig route = createValidRoute();
        route.setId(null);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Route ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateRouteWithEmptyId() {
        RouteConfig route = createValidRoute();
        route.setId("");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Route ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateRouteWithInvalidUri() {
        RouteConfig route = createValidRoute();
        route.setUri("ht tp://invalid uri with spaces");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertTrue(exception.getMessage().contains("Invalid URI format"));
    }

    @Test
    void testValidateRouteWithNullUri() {
        RouteConfig route = createValidRoute();
        route.setUri(null);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Route URI cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateRouteWithNoPredicates() {
        RouteConfig route = createValidRoute();
        route.setPredicates(Collections.emptyList());
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Route must have at least one predicate", exception.getMessage());
    }

    @Test
    void testValidateRouteWithUnsupportedPredicate() {
        RouteConfig route = createValidRoute();
        PredicateConfig invalidPredicate = new PredicateConfig("UnsupportedPredicate", new HashMap<>());
        route.setPredicates(Collections.singletonList(invalidPredicate));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertTrue(exception.getMessage().contains("Unsupported predicate: UnsupportedPredicate"));
    }

    @Test
    void testValidatePathPredicateWithoutPattern() {
        RouteConfig route = createValidRoute();
        PredicateConfig pathPredicate = new PredicateConfig("Path", new HashMap<>());
        route.setPredicates(Collections.singletonList(pathPredicate));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Path predicate must have arguments", exception.getMessage());
    }

    @Test
    void testValidatePathPredicateWithInvalidPattern() {
        RouteConfig route = createValidRoute();
        Map<String, String> args = new HashMap<>();
        args.put("pattern", "invalid-pattern"); // Should start with /
        PredicateConfig pathPredicate = new PredicateConfig("Path", args);
        route.setPredicates(Collections.singletonList(pathPredicate));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertTrue(exception.getMessage().contains("Path pattern must start with '/'"));
    }

    @Test
    void testValidateRouteWithUnsupportedFilter() {
        RouteConfig route = createValidRoute();
        FilterConfig invalidFilter = new FilterConfig("UnsupportedFilter", new HashMap<>());
        route.setFilters(Collections.singletonList(invalidFilter));
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertTrue(exception.getMessage().contains("Unsupported filter: UnsupportedFilter"));
    }

    @Test
    void testValidateRouteWithNegativeTimeout() {
        RouteConfig route = createValidRoute();
        RouteMetadata metadata = new RouteMetadata(-1, true, 0);
        route.setMetadata(metadata);
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoute(route)
        );
        assertEquals("Route timeout cannot be negative", exception.getMessage());
    }

    @Test
    void testValidateRoutesWithDuplicateIds() {
        RouteConfig route1 = createValidRoute();
        RouteConfig route2 = createValidRoute();
        route2.setId("test-route"); // Same ID as route1
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoutes(Arrays.asList(route1, route2))
        );
        assertEquals("Duplicate route ID found: test-route", exception.getMessage());
    }

    @Test
    void testValidateEmptyRoutes() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, 
            () -> validator.validateRoutes(Collections.emptyList())
        );
        assertEquals("Routes configuration cannot be null or empty", exception.getMessage());
    }

    private RouteConfig createValidRoute() {
        Map<String, String> predicateArgs = new HashMap<>();
        predicateArgs.put("pattern", "/test/**");
        PredicateConfig predicate = new PredicateConfig("Path", predicateArgs);
        
        Map<String, Object> filterArgs = new HashMap<>();
        filterArgs.put("name", "test-cb");
        FilterConfig filter = new FilterConfig("CircuitBreaker", filterArgs);
        
        RouteMetadata metadata = new RouteMetadata(5000, true, 0);
        
        return new RouteConfig(
            "test-route",
            "http://localhost:8080",
            Collections.singletonList(predicate),
            Collections.singletonList(filter),
            metadata
        );
    }
}