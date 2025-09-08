package com.example.gateway.service;

import com.example.gateway.config.DynamicRouteLocator;
import com.example.gateway.config.GatewayRoutingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Service responsible for handling route refresh operations.
 * Listens for configuration refresh events and triggers route reloading.
 */
@Service
public class RouteRefreshService {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteRefreshService.class);
    
    private final ApplicationEventPublisher eventPublisher;
    private final GatewayRoutingProperties routingProperties;
    private final DynamicRouteLocator dynamicRouteLocator;
    private final ContextRefresher contextRefresher;
    
    public RouteRefreshService(ApplicationEventPublisher eventPublisher,
                              GatewayRoutingProperties routingProperties,
                              DynamicRouteLocator dynamicRouteLocator,
                              ContextRefresher contextRefresher) {
        this.eventPublisher = eventPublisher;
        this.routingProperties = routingProperties;
        this.dynamicRouteLocator = dynamicRouteLocator;
        this.contextRefresher = contextRefresher;
    }
    
    /**
     * Manually refresh routes by reloading configuration and publishing refresh event
     */
    public Mono<Void> refreshRoutes() {
        logger.info("Manually refreshing gateway routes");
        
        return Mono.fromRunnable(() -> {
            try {
                // Refresh the application context to reload @RefreshScope beans
                Set<String> refreshedKeys = contextRefresher.refresh();
                logger.info("Refreshed configuration keys: {}", refreshedKeys);
                
                // Publish route refresh event to trigger route reloading
                eventPublisher.publishEvent(new RefreshRoutesEvent(this));
                logger.info("Published RefreshRoutesEvent to reload routes");
                
            } catch (Exception e) {
                logger.error("Error during route refresh: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to refresh routes", e);
            }
        });
    }
    
    /**
     * Listen for refresh events and log route changes
     */
    @EventListener
    public void handleRefreshRoutesEvent(RefreshRoutesEvent event) {
        logger.info("Received RefreshRoutesEvent, routes will be reloaded");
        
        // Log current route configuration for debugging
        if (routingProperties.getRoutes() != null) {
            logger.info("Current route configuration contains {} routes", 
                    routingProperties.getRoutes().size());
            
            routingProperties.getRoutes().forEach(route -> {
                logger.debug("Route: {} -> {} (enabled: {})", 
                        route.getId(), 
                        route.getUri(), 
                        dynamicRouteLocator.isRouteEnabled(route));
            });
        } else {
            logger.warn("No routes found in current configuration");
        }
    }
    
    /**
     * Get current route count for monitoring
     */
    public int getCurrentRouteCount() {
        if (routingProperties.getRoutes() != null) {
            return (int) routingProperties.getRoutes().stream()
                    .filter(dynamicRouteLocator::isRouteEnabled)
                    .count();
        }
        return 0;
    }
    
    /**
     * Check if a specific route exists and is enabled
     */
    public boolean isRouteEnabled(String routeId) {
        if (routingProperties.getRoutes() != null) {
            return routingProperties.getRoutes().stream()
                    .filter(route -> routeId.equals(route.getId()))
                    .findFirst()
                    .map(dynamicRouteLocator::isRouteEnabled)
                    .orElse(false);
        }
        return false;
    }
}