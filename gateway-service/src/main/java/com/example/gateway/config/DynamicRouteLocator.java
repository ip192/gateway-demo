package com.example.gateway.config;

import com.example.gateway.model.FilterConfig;
import com.example.gateway.model.PredicateConfig;
import com.example.gateway.model.RouteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dynamic route locator that creates routes based on configuration properties.
 * Implements RouteLocator interface to provide routes to Spring Cloud Gateway.
 * Supports hot reload via @RefreshScope annotation.
 */
@Component
@RefreshScope
public class DynamicRouteLocator implements RouteLocator {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicRouteLocator.class);
    
    private final GatewayRoutingProperties routingProperties;
    private final RouteLocatorBuilder routeLocatorBuilder;
    
    public DynamicRouteLocator(GatewayRoutingProperties routingProperties, 
                              RouteLocatorBuilder routeLocatorBuilder) {
        this.routingProperties = routingProperties;
        this.routeLocatorBuilder = routeLocatorBuilder;
    }
    
    @Override
    public Flux<Route> getRoutes() {
        logger.info("Loading dynamic routes from configuration");
        
        if (routingProperties.getRoutes() == null || routingProperties.getRoutes().isEmpty()) {
            logger.info("No route configurations found");
            return Flux.empty();
        }
        
        List<Route> routes = new ArrayList<>();
        
        // Sort routes by order (priority) - lower order values have higher priority
        List<RouteConfig> sortedRoutes = routingProperties.getRoutes().stream()
                .sorted((r1, r2) -> Integer.compare(getRouteOrder(r1), getRouteOrder(r2)))
                .collect(Collectors.toList());
        
        logger.info("Found {} route configurations", sortedRoutes.size());
        
        for (RouteConfig routeConfig : sortedRoutes) {
            if (!isRouteEnabled(routeConfig)) {
                logger.debug("Route {} is disabled, skipping", routeConfig.getId());
                continue;
            }
            
            if (!validateRouteConfig(routeConfig)) {
                logger.error("Route {} has invalid configuration, skipping", routeConfig.getId());
                continue;
            }
            
            try {
                Route route = createRoute(routeConfig);
                if (route != null) {
                    routes.add(route);
                    logger.debug("Created route {} with order {} pointing to {}", 
                            routeConfig.getId(), getRouteOrder(routeConfig), routeConfig.getUri());
                } else {
                    logger.warn("Failed to create route {} - route creation returned null", routeConfig.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to create route {}: {}", routeConfig.getId(), e.getMessage(), e);
            }
        }
        
        logger.info("Successfully created {} dynamic routes", routes.size());
        return Flux.fromIterable(routes);
    }
    
    /**
     * Creates a Route object from RouteConfig using RouteDefinition
     */
    private Route createRoute(RouteConfig routeConfig) {
        try {
            // For the older Spring Cloud Gateway version, we'll create a simple route
            String pathPattern = "/**"; // Default pattern
            
            // Extract path pattern from predicates
            if (routeConfig.getPredicates() != null && !routeConfig.getPredicates().isEmpty()) {
                for (PredicateConfig predicateConfig : routeConfig.getPredicates()) {
                    if ("Path".equalsIgnoreCase(predicateConfig.getName())) {
                        String pattern = predicateConfig.getArgs().get("pattern");
                        if (pattern == null) {
                            pattern = predicateConfig.getArgs().get("_genkey_0");
                        }
                        if (pattern != null && !pattern.trim().isEmpty()) {
                            pathPattern = pattern;
                            break;
                        }
                    }
                }
            }
            
            final String finalPathPattern = pathPattern;
            final String routeId = routeConfig.getId();
            final String routeUri = routeConfig.getUri();
            final int routeOrder = getRouteOrder(routeConfig);
            
            // Create a simple route using RouteLocatorBuilder
            RouteLocator routeLocator = routeLocatorBuilder.routes()
                    .route(routeId, r -> 
                        r.order(routeOrder)
                         .path(finalPathPattern)
                         .uri(routeUri)
                    )
                    .build();
            
            // Get the first (and only) route from the locator
            return routeLocator.getRoutes().blockFirst();
            
        } catch (Exception e) {
            logger.error("Failed to create route using RouteLocatorBuilder for {}: {}", 
                    routeConfig.getId(), e.getMessage());
            
            // Fallback: create a basic Route manually
            return createBasicRoute(routeConfig);
        }
    }
    
    /**
     * Creates a basic Route object manually as fallback
     */
    private Route createBasicRoute(RouteConfig routeConfig) {
        try {
            Route.AsyncBuilder routeBuilder = Route.async()
                    .id(routeConfig.getId())
                    .uri(URI.create(routeConfig.getUri()))
                    .order(getRouteOrder(routeConfig));
            
            // Add a simple path predicate
            if (routeConfig.getPredicates() != null && !routeConfig.getPredicates().isEmpty()) {
                PredicateConfig firstPredicate = routeConfig.getPredicates().get(0);
                if ("Path".equalsIgnoreCase(firstPredicate.getName())) {
                    String pattern = firstPredicate.getArgs().get("pattern");
                    if (pattern == null) {
                        pattern = firstPredicate.getArgs().get("_genkey_0");
                    }
                    if (pattern != null) {
                        // For now, we'll just log the pattern since we can't easily add predicates to Route.AsyncBuilder
                        logger.debug("Route {} configured with path pattern: {}", routeConfig.getId(), pattern);
                    }
                }
            }
            
            return routeBuilder.build();
            
        } catch (Exception e) {
            logger.error("Failed to create basic route for {}: {}", routeConfig.getId(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a PredicateDefinition from PredicateConfig
     */
    private PredicateDefinition createPredicateDefinition(PredicateConfig predicateConfig) {
        PredicateDefinition predicateDefinition = new PredicateDefinition();
        predicateDefinition.setName(predicateConfig.getName());
        
        if (predicateConfig.getArgs() != null) {
            predicateDefinition.setArgs(predicateConfig.getArgs());
        }
        
        return predicateDefinition;
    }
    
    /**
     * Creates a FilterDefinition from FilterConfig
     */
    private FilterDefinition createFilterDefinition(FilterConfig filterConfig) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(filterConfig.getName());
        
        if (filterConfig.getArgs() != null) {
            // Convert Object values to String for FilterDefinition
            filterConfig.getArgs().forEach((key, value) -> {
                if (value != null) {
                    filterDefinition.addArg(key, value.toString());
                }
            });
        }
        
        return filterDefinition;
    }
    
    /**
     * Validates a predicate configuration
     */
    public boolean validatePredicate(PredicateConfig predicateConfig) {
        String predicateName = predicateConfig.getName();
        
        switch (predicateName.toLowerCase()) {
            case "path":
                String pathPattern = predicateConfig.getArgs().get("pattern");
                if (pathPattern == null) {
                    pathPattern = predicateConfig.getArgs().get("_genkey_0");
                }
                return pathPattern != null && !pathPattern.trim().isEmpty();
                
            case "method":
                String method = predicateConfig.getArgs().get("method");
                if (method == null) {
                    method = predicateConfig.getArgs().get("_genkey_0");
                }
                return method != null && !method.trim().isEmpty();
                
            case "header":
                String headerName = predicateConfig.getArgs().get("header");
                return headerName != null && !headerName.trim().isEmpty();
                
            case "query":
                String paramName = predicateConfig.getArgs().get("param");
                return paramName != null && !paramName.trim().isEmpty();
                
            default:
                logger.warn("Unsupported predicate type: {}", predicateName);
                return false;
        }
    }
    
    /**
     * Validates a filter configuration
     */
    public boolean validateFilter(FilterConfig filterConfig) {
        String filterName = filterConfig.getName();
        
        switch (filterName.toLowerCase()) {
            case "circuitbreaker":
                String cbName = (String) filterConfig.getArgs().get("name");
                return cbName != null && !cbName.trim().isEmpty();
                
            case "retry":
                Object retriesObj = filterConfig.getArgs().get("retries");
                if (retriesObj != null) {
                    try {
                        int retries = retriesObj instanceof Integer ? (Integer) retriesObj : 
                                     Integer.parseInt(retriesObj.toString());
                        return retries > 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
                
            case "stripprefix":
                Object partsObj = filterConfig.getArgs().get("parts");
                if (partsObj != null) {
                    try {
                        int parts = partsObj instanceof Integer ? (Integer) partsObj : 
                                   Integer.parseInt(partsObj.toString());
                        return parts >= 0;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
                
            case "addrequesteader":
            case "addresponseheader":
                String headerName = (String) filterConfig.getArgs().get("name");
                String headerValue = (String) filterConfig.getArgs().get("value");
                return headerName != null && !headerName.trim().isEmpty() && 
                       headerValue != null && !headerValue.trim().isEmpty();
                
            default:
                logger.warn("Unsupported filter type: {}", filterName);
                return false;
        }
    }
    
    /**
     * Validates a complete route configuration
     */
    public boolean validateRouteConfig(RouteConfig routeConfig) {
        if (routeConfig.getId() == null || routeConfig.getId().trim().isEmpty()) {
            logger.error("Route ID is required");
            return false;
        }
        
        if (routeConfig.getUri() == null || routeConfig.getUri().trim().isEmpty()) {
            logger.error("Route URI is required for route: {}", routeConfig.getId());
            return false;
        }
        
        // Validate predicates
        if (routeConfig.getPredicates() != null) {
            for (PredicateConfig predicate : routeConfig.getPredicates()) {
                if (!validatePredicate(predicate)) {
                    logger.error("Invalid predicate configuration in route: {}", routeConfig.getId());
                    return false;
                }
            }
        }
        
        // Validate filters
        if (routeConfig.getFilters() != null) {
            for (FilterConfig filter : routeConfig.getFilters()) {
                if (!validateFilter(filter)) {
                    logger.error("Invalid filter configuration in route: {}", routeConfig.getId());
                    return false;
                }
            }
        }
        
        return true;
    }
    

    
    /**
     * Gets the priority order for a route based on its metadata
     */
    public int getRouteOrder(RouteConfig routeConfig) {
        if (routeConfig.getMetadata() != null) {
            return routeConfig.getMetadata().getOrder();
        }
        return 0;
    }
    
    /**
     * Checks if a route is enabled based on its metadata
     */
    public boolean isRouteEnabled(RouteConfig routeConfig) {
        if (routeConfig.getMetadata() != null) {
            return routeConfig.getMetadata().isEnabled();
        }
        return true; // Default to enabled
    }
}