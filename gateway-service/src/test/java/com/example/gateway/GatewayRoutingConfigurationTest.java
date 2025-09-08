package com.example.gateway;

import com.example.gateway.config.GatewayRoutingConfiguration;
import com.example.gateway.config.GatewayRoutingProperties;
import com.example.gateway.config.RouteConfigValidator;
import com.example.gateway.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GatewayRoutingConfigurationTest {

    @Mock
    private GatewayRoutingProperties gatewayRoutingProperties;
    
    @Mock
    private RouteConfigValidator routeConfigValidator;
    
    @Mock
    private ApplicationReadyEvent applicationReadyEvent;
    
    private GatewayRoutingConfiguration configuration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        configuration = new GatewayRoutingConfiguration(gatewayRoutingProperties, routeConfigValidator);
    }

    @Test
    void testValidateConfigurationWithValidRoutes() {
        RouteConfig validRoute = createValidRoute();
        when(gatewayRoutingProperties.getRoutes()).thenReturn(Collections.singletonList(validRoute));
        
        assertDoesNotThrow(() -> configuration.validateConfiguration());
        
        verify(routeConfigValidator).validateRoutes(Collections.singletonList(validRoute));
    }

    @Test
    void testValidateConfigurationWithNoRoutes() {
        when(gatewayRoutingProperties.getRoutes()).thenReturn(Collections.emptyList());
        
        assertDoesNotThrow(() -> configuration.validateConfiguration());
        
        verify(routeConfigValidator, never()).validateRoutes(any());
    }

    @Test
    void testValidateConfigurationWithNullRoutes() {
        when(gatewayRoutingProperties.getRoutes()).thenReturn(null);
        
        assertDoesNotThrow(() -> configuration.validateConfiguration());
        
        verify(routeConfigValidator, never()).validateRoutes(any());
    }

    @Test
    void testValidateConfigurationWithInvalidRoutes() {
        RouteConfig invalidRoute = createValidRoute();
        when(gatewayRoutingProperties.getRoutes()).thenReturn(Collections.singletonList(invalidRoute));
        doThrow(new IllegalArgumentException("Invalid route configuration"))
            .when(routeConfigValidator).validateRoutes(any());
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> configuration.validateConfiguration()
        );
        
        assertEquals("Invalid gateway routing configuration", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertEquals("Invalid route configuration", exception.getCause().getMessage());
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