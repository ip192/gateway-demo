package com.example.gateway.config;

import com.example.gateway.model.RouteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Optimized route locator with caching and performance improvements.
 * Reduces route matching overhead through intelligent caching and pre-processing.
 */
@Component("optimizedRouteLocator")
@RefreshScope
public class OptimizedRouteLocator implements RouteLocator {
    
    private static final Logger logger = LoggerFactory.getLogger(OptimizedRouteLocator.class);
    
    private final GatewayRoutingProperties routingProperties;
    private final RouteLocatorBuilder routeLocatorBuilder;
    
    // Cache for compiled routes to avoid recompilation
    private volatile Map<String, Route> routeCache = new ConcurrentHashMap<>();
    private volatile long lastConfigHash = 0;
    
    public OptimizedRouteLocator(GatewayRoutingProperties routingProperties, 
                                RouteLocatorBuilder routeLocatorBuilder) {
        this.routingProperties = routingProperties;
        this.routeLocatorBuilder = routeLocatorBuilder;
    }
    
    @Override
    public Flux<Route> getRoutes() {
        // Check if configuration has changed
        long currentConfigHash = calculateConfigHash();
        
        if (currentConfigHash != lastConfigHash || routeCache.isEmpty()) {
            synchronized (this) {
                // Double-check locking pattern
                if (currentConfigHash != lastConfigHash || routeCache.isEmpty()) {
                    logger.info("Route configuration changed, rebuilding cache");
                    rebuildRouteCache();
                    lastConfigHash = currentConfigHash;
                }
            }
        }
        
        return Flux.fromIterable(routeCache.values());
    }
    
    private void rebuildRouteCache() {
        Map<String, Route> newCache = new ConcurrentHashMap<>();
        
        if (routingProperties.getRoutes() == null || routingProperties.getRoutes().isEmpty()) {
            logger.info("No route configurations found");
            routeCache = newCache;
            return;
        }
        
        // Sort routes by priority for optimal matching order
        List<RouteConfig> sortedRoutes = routingProperties.getRoutes().stream()
                .filter(this::isRouteEnabled)
                .filter(this::validateRouteConfig)
                .sorted((r1, r2) -> Integer.compare(getRouteOrder(r1), getRouteOrder(r2)))
                .collect(Collectors.toList());
        
        logger.info("Building {} optimized routes", sortedRoutes.size());
        
        for (RouteConfig routeConfig : sortedRoutes) {
            try {
                Route route = createOptimizedRoute(routeConfig);
                if (route != null) {
                    newCache.put(routeConfig.getId(), route);
                    logger.debug("Cached optimized route: {} -> {}", 
                            routeConfig.getId(), routeConfig.getUri());
                }
            } catch (Exception e) {
                logger.error("Failed to create optimized route {}: {}", 
                        routeConfig.getId(), e.getMessage());
            }
        }
        
        routeCache = newCache;
        logger.info("Route cache rebuilt with {} routes", newCache.size());
    }
    
    private Route createOptimizedRoute(RouteConfig routeConfig) {
        try {
            // Extract path pattern efficiently
            String pathPattern = extractPathPattern(routeConfig);
            
            // Create route with minimal overhead
            RouteLocator routeLocator = routeLocatorBuilder.routes()
                    .route(routeConfig.getId(), r -> r.order(getRouteOrder(routeConfig))
                                        .path(pathPattern)
                                        .uri(routeConfig.getUri()))
                    .build();
            
            return routeLocator.getRoutes().blockFirst();
            
        } catch (Exception e) {
            logger.error("Failed to create optimized route for {}: {}", 
                    routeConfig.getId(), e.getMessage());
            return null;
        }
    }
    
    private String extractPathPattern(RouteConfig routeConfig) {
        if (routeConfig.getPredicates() != null) {
            return routeConfig.getPredicates().stream()
                    .filter(p -> "Path".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .map(p -> {
                        String pattern = p.getArgs().get("pattern");
                        return pattern != null ? pattern : p.getArgs().get("_genkey_0");
                    })
                    .orElse("/**");
        }
        return "/**";
    }
    
    private String extractMethodPredicate(RouteConfig routeConfig) {
        if (routeConfig.getPredicates() != null) {
            return routeConfig.getPredicates().stream()
                    .filter(p -> "Method".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .map(p -> {
                        String method = p.getArgs().get("method");
                        return method != null ? method : p.getArgs().get("_genkey_0");
                    })
                    .orElse(null);
        }
        return null;
    }
    
    private String[] extractHeaderPredicate(RouteConfig routeConfig) {
        if (routeConfig.getPredicates() != null) {
            return routeConfig.getPredicates().stream()
                    .filter(p -> "Header".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .map(p -> {
                        String header = p.getArgs().get("header");
                        String regexp = p.getArgs().get("regexp");
                        if (header != null && regexp != null) {
                            return new String[]{header, regexp};
                        }
                        return null;
                    })
                    .orElse(null);
        }
        return null;
    }
    
    private long calculateConfigHash() {
        if (routingProperties.getRoutes() == null) {
            return 0;
        }
        
        // Simple hash based on route configurations
        return routingProperties.getRoutes().stream()
                .mapToLong(route -> {
                    long hash = route.getId().hashCode();
                    hash = hash * 31 + route.getUri().hashCode();
                    if (route.getPredicates() != null) {
                        hash = hash * 31 + route.getPredicates().hashCode();
                    }
                    if (route.getMetadata() != null) {
                        hash = hash * 31 + Boolean.hashCode(route.getMetadata().isEnabled());
                        hash = hash * 31 + Integer.hashCode(route.getMetadata().getOrder());
                    }
                    return hash;
                })
                .sum();
    }
    
    private boolean validateRouteConfig(RouteConfig routeConfig) {
        if (routeConfig.getId() == null || routeConfig.getId().trim().isEmpty()) {
            logger.error("Route ID is required");
            return false;
        }
        
        if (routeConfig.getUri() == null || routeConfig.getUri().trim().isEmpty()) {
            logger.error("Route URI is required for route: {}", routeConfig.getId());
            return false;
        }
        
        return true;
    }
    
    private int getRouteOrder(RouteConfig routeConfig) {
        if (routeConfig.getMetadata() != null) {
            return routeConfig.getMetadata().getOrder();
        }
        return 0;
    }
    
    private boolean isRouteEnabled(RouteConfig routeConfig) {
        if (routeConfig.getMetadata() != null) {
            return routeConfig.getMetadata().isEnabled();
        }
        return true;
    }
    
    /**
     * Clear the route cache - useful for testing or manual refresh
     */
    public void clearCache() {
        synchronized (this) {
            routeCache.clear();
            lastConfigHash = 0;
            logger.info("Route cache cleared");
        }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        return new CacheStats(routeCache.size(), lastConfigHash);
    }
    
    public static class CacheStats {
        private final int cachedRoutes;
        private final long configHash;
        
        public CacheStats(int cachedRoutes, long configHash) {
            this.cachedRoutes = cachedRoutes;
            this.configHash = configHash;
        }
        
        public int getCachedRoutes() { return cachedRoutes; }
        public long getConfigHash() { return configHash; }
    }
}