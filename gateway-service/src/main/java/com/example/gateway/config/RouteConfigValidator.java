package com.example.gateway.config;

import com.example.gateway.model.FilterConfig;
import com.example.gateway.model.PredicateConfig;
import com.example.gateway.model.RouteConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator for route configuration to ensure correctness and consistency
 */
@Component
public class RouteConfigValidator {

    private static final Set<String> SUPPORTED_PREDICATES = new HashSet<>(Arrays.asList(
        "Path", "Method", "Header", "Query", "Host", "Cookie", "After", "Before", "Between"
    ));

    private static final Set<String> SUPPORTED_FILTERS = new HashSet<>(Arrays.asList(
        "CircuitBreaker", "Retry", "RequestRateLimiter", "AddRequestHeader", 
        "AddResponseHeader", "RewritePath", "StripPrefix", "PrefixPath"
    ));

    /**
     * Validates a list of route configurations
     * @param routes List of route configurations to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateRoutes(List<RouteConfig> routes) {
        if (routes == null || routes.isEmpty()) {
            throw new IllegalArgumentException("Routes configuration cannot be null or empty");
        }

        Set<String> routeIds = new HashSet<>();
        
        for (RouteConfig route : routes) {
            validateRoute(route);
            
            // Check for duplicate route IDs
            if (routeIds.contains(route.getId())) {
                throw new IllegalArgumentException("Duplicate route ID found: " + route.getId());
            }
            routeIds.add(route.getId());
        }
    }

    /**
     * Validates a single route configuration
     * @param route Route configuration to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateRoute(RouteConfig route) {
        if (route == null) {
            throw new IllegalArgumentException("Route configuration cannot be null");
        }

        // Validate route ID
        if (!StringUtils.hasText(route.getId())) {
            throw new IllegalArgumentException("Route ID cannot be null or empty");
        }

        // Validate URI
        validateUri(route.getUri());

        // Validate predicates
        validatePredicates(route.getPredicates());

        // Validate filters
        validateFilters(route.getFilters());

        // Validate metadata
        validateMetadata(route);
    }

    private void validateUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            throw new IllegalArgumentException("Route URI cannot be null or empty");
        }

        try {
            new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI format: " + uri, e);
        }
    }

    private void validatePredicates(List<PredicateConfig> predicates) {
        if (predicates == null || predicates.isEmpty()) {
            throw new IllegalArgumentException("Route must have at least one predicate");
        }

        for (PredicateConfig predicate : predicates) {
            if (predicate == null) {
                throw new IllegalArgumentException("Predicate configuration cannot be null");
            }

            if (!StringUtils.hasText(predicate.getName())) {
                throw new IllegalArgumentException("Predicate name cannot be null or empty");
            }

            if (!SUPPORTED_PREDICATES.contains(predicate.getName())) {
                throw new IllegalArgumentException("Unsupported predicate: " + predicate.getName() + 
                    ". Supported predicates: " + SUPPORTED_PREDICATES);
            }

            // Validate Path predicate specifically
            if ("Path".equals(predicate.getName())) {
                validatePathPredicate(predicate);
            }
        }
    }

    private void validatePathPredicate(PredicateConfig predicate) {
        if (predicate.getArgs() == null || predicate.getArgs().isEmpty()) {
            throw new IllegalArgumentException("Path predicate must have arguments");
        }

        String pattern = predicate.getArgs().get("pattern");
        if (!StringUtils.hasText(pattern)) {
            throw new IllegalArgumentException("Path predicate must have a 'pattern' argument");
        }

        // Basic path pattern validation
        if (!pattern.startsWith("/")) {
            throw new IllegalArgumentException("Path pattern must start with '/': " + pattern);
        }
    }

    private void validateFilters(List<FilterConfig> filters) {
        if (filters == null) {
            return; // Filters are optional
        }

        for (FilterConfig filter : filters) {
            if (filter == null) {
                throw new IllegalArgumentException("Filter configuration cannot be null");
            }

            if (!StringUtils.hasText(filter.getName())) {
                throw new IllegalArgumentException("Filter name cannot be null or empty");
            }

            if (!SUPPORTED_FILTERS.contains(filter.getName())) {
                throw new IllegalArgumentException("Unsupported filter: " + filter.getName() + 
                    ". Supported filters: " + SUPPORTED_FILTERS);
            }
        }
    }

    private void validateMetadata(RouteConfig route) {
        if (route.getMetadata() != null) {
            if (route.getMetadata().getTimeout() < 0) {
                throw new IllegalArgumentException("Route timeout cannot be negative");
            }
            
            if (route.getMetadata().getOrder() < 0) {
                throw new IllegalArgumentException("Route order cannot be negative");
            }
        }
    }
}