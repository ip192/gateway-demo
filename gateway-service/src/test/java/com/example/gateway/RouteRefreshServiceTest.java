package com.example.gateway;

import com.example.gateway.config.DynamicRouteLocator;
import com.example.gateway.config.GatewayRoutingProperties;
import com.example.gateway.model.RouteConfig;
import com.example.gateway.model.RouteMetadata;
import com.example.gateway.service.RouteRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteRefreshServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GatewayRoutingProperties routingProperties;

    @Mock
    private DynamicRouteLocator dynamicRouteLocator;

    @Mock
    private ContextRefresher contextRefresher;

    private RouteRefreshService routeRefreshService;

    @BeforeEach
    void setUp() {
        routeRefreshService = new RouteRefreshService(
                eventPublisher,
                routingProperties,
                dynamicRouteLocator,
                contextRefresher
        );
    }

    @Test
    void testRefreshRoutes_Success() {
        // Given
        Set<String> refreshedKeys = new HashSet<>(Arrays.asList("gateway.routes[0].uri", "gateway.routes[1].enabled"));
        when(contextRefresher.refresh()).thenReturn(refreshedKeys);

        // When & Then
        StepVerifier.create(routeRefreshService.refreshRoutes())
                .verifyComplete();

        verify(contextRefresher).refresh();
        verify(eventPublisher).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void testRefreshRoutes_ContextRefresherThrowsException() {
        // Given
        when(contextRefresher.refresh()).thenThrow(new RuntimeException("Context refresh failed"));

        // When & Then
        StepVerifier.create(routeRefreshService.refreshRoutes())
                .expectError(RuntimeException.class)
                .verify();

        verify(contextRefresher).refresh();
        verify(eventPublisher, never()).publishEvent(any(RefreshRoutesEvent.class));
    }

    @Test
    void testGetCurrentRouteCount_WithEnabledRoutes() {
        // Given
        List<RouteConfig> routes = Arrays.asList(
                createRouteConfig("route1", true),
                createRouteConfig("route2", true),
                createRouteConfig("route3", false)
        );
        when(routingProperties.getRoutes()).thenReturn(routes);
        when(dynamicRouteLocator.isRouteEnabled(routes.get(0))).thenReturn(true);
        when(dynamicRouteLocator.isRouteEnabled(routes.get(1))).thenReturn(true);
        when(dynamicRouteLocator.isRouteEnabled(routes.get(2))).thenReturn(false);

        // When
        int count = routeRefreshService.getCurrentRouteCount();

        // Then
        assertEquals(2, count);
    }

    @Test
    void testGetCurrentRouteCount_NoRoutes() {
        // Given
        when(routingProperties.getRoutes()).thenReturn(null);

        // When
        int count = routeRefreshService.getCurrentRouteCount();

        // Then
        assertEquals(0, count);
    }

    @Test
    void testIsRouteEnabled_ExistingEnabledRoute() {
        // Given
        RouteConfig enabledRoute = createRouteConfig("test-route", true);
        List<RouteConfig> routes = Arrays.asList(enabledRoute);
        when(routingProperties.getRoutes()).thenReturn(routes);
        when(dynamicRouteLocator.isRouteEnabled(enabledRoute)).thenReturn(true);

        // When
        boolean enabled = routeRefreshService.isRouteEnabled("test-route");

        // Then
        assertTrue(enabled);
    }

    @Test
    void testIsRouteEnabled_ExistingDisabledRoute() {
        // Given
        RouteConfig disabledRoute = createRouteConfig("test-route", false);
        List<RouteConfig> routes = Arrays.asList(disabledRoute);
        when(routingProperties.getRoutes()).thenReturn(routes);
        when(dynamicRouteLocator.isRouteEnabled(disabledRoute)).thenReturn(false);

        // When
        boolean enabled = routeRefreshService.isRouteEnabled("test-route");

        // Then
        assertFalse(enabled);
    }

    @Test
    void testIsRouteEnabled_NonExistingRoute() {
        // Given
        List<RouteConfig> routes = Arrays.asList(createRouteConfig("other-route", true));
        when(routingProperties.getRoutes()).thenReturn(routes);

        // When
        boolean enabled = routeRefreshService.isRouteEnabled("non-existing-route");

        // Then
        assertFalse(enabled);
    }

    @Test
    void testIsRouteEnabled_NoRoutes() {
        // Given
        when(routingProperties.getRoutes()).thenReturn(null);

        // When
        boolean enabled = routeRefreshService.isRouteEnabled("any-route");

        // Then
        assertFalse(enabled);
    }

    @Test
    void testHandleRefreshRoutesEvent() {
        // Given
        List<RouteConfig> routes = Arrays.asList(
                createRouteConfig("route1", true),
                createRouteConfig("route2", false)
        );
        when(routingProperties.getRoutes()).thenReturn(routes);
        when(dynamicRouteLocator.isRouteEnabled(any())).thenReturn(true, false);

        RefreshRoutesEvent event = new RefreshRoutesEvent(this);

        // When
        routeRefreshService.handleRefreshRoutesEvent(event);

        // Then - No exception should be thrown, method should complete successfully
        verify(routingProperties).getRoutes();
    }

    @Test
    void testHandleRefreshRoutesEvent_NoRoutes() {
        // Given
        when(routingProperties.getRoutes()).thenReturn(null);
        RefreshRoutesEvent event = new RefreshRoutesEvent(this);

        // When
        routeRefreshService.handleRefreshRoutesEvent(event);

        // Then - No exception should be thrown
        verify(routingProperties).getRoutes();
    }

    private RouteConfig createRouteConfig(String id, boolean enabled) {
        RouteConfig route = new RouteConfig();
        route.setId(id);
        route.setUri("http://localhost:8080");
        
        RouteMetadata metadata = new RouteMetadata();
        metadata.setEnabled(enabled);
        route.setMetadata(metadata);
        
        return route;
    }
}