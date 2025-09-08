package com.example.gateway;

import com.example.gateway.config.GatewayRoutingProperties;
import com.example.gateway.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GatewayRoutingPropertiesTest {

    @Test
    void testGatewayRoutingPropertiesCreation() {
        GatewayRoutingProperties properties = new GatewayRoutingProperties();
        
        assertNotNull(properties);
        assertNull(properties.getRoutes());
        assertNull(properties.getCircuitBreaker());
        assertNull(properties.getRetry());
    }

    @Test
    void testSetAndGetRoutes() {
        GatewayRoutingProperties properties = new GatewayRoutingProperties();
        RouteConfig route = createTestRoute();
        
        properties.setRoutes(Collections.singletonList(route));
        
        assertNotNull(properties.getRoutes());
        assertEquals(1, properties.getRoutes().size());
        assertEquals("test-route", properties.getRoutes().get(0).getId());
    }

    @Test
    void testSetAndGetCircuitBreaker() {
        GatewayRoutingProperties properties = new GatewayRoutingProperties();
        CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig(60, 15000, 15, 8);
        
        properties.setCircuitBreaker(circuitBreaker);
        
        assertNotNull(properties.getCircuitBreaker());
        assertEquals(60, properties.getCircuitBreaker().getFailureRateThreshold());
        assertEquals(15000, properties.getCircuitBreaker().getWaitDurationInOpenState());
    }

    @Test
    void testSetAndGetRetry() {
        GatewayRoutingProperties properties = new GatewayRoutingProperties();
        RetryConfig retry = new RetryConfig(5, 100, 1000, 1.5);
        
        properties.setRetry(retry);
        
        assertNotNull(properties.getRetry());
        assertEquals(5, properties.getRetry().getRetries());
        assertEquals(100, properties.getRetry().getFirstBackoff());
    }

    @Test
    void testToString() {
        GatewayRoutingProperties properties = new GatewayRoutingProperties();
        RouteConfig route = createTestRoute();
        CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
        RetryConfig retry = new RetryConfig();
        
        properties.setRoutes(Collections.singletonList(route));
        properties.setCircuitBreaker(circuitBreaker);
        properties.setRetry(retry);
        
        String toString = properties.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("GatewayRoutingProperties"));
        assertTrue(toString.contains("routes="));
        assertTrue(toString.contains("circuitBreaker="));
        assertTrue(toString.contains("retry="));
    }

    private RouteConfig createTestRoute() {
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