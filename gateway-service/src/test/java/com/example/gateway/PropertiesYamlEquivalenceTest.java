package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that properties loading matches original YAML behavior
 * This test validates that the converted properties files produce the same configuration
 * as the original YAML files would have produced
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PropertiesYamlEquivalenceTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    private WebEndpointProperties webEndpointProperties;

    @Test
    void testUserServiceRouteEquivalence() {
        // Verify user service route configuration matches expected YAML equivalent
        RouteDefinition userRoute = findRouteById("user-service-routes");
        assertThat(userRoute).isNotNull();
        
        // Verify URI
        assertThat(userRoute.getUri().toString()).isEqualTo("http://localhost:8081");
        
        // Verify predicates
        assertThat(userRoute.getPredicates()).hasSize(1);
        PredicateDefinition pathPredicate = userRoute.getPredicates().get(0);
        assertThat(pathPredicate.getName()).isEqualTo("Path");
        assertThat(pathPredicate.getArgs().values()).contains("/user/**");
        
        // Verify filters
        assertThat(userRoute.getFilters()).hasSize(2);
        
        // Verify CircuitBreaker filter
        FilterDefinition cbFilter = findFilterByName(userRoute.getFilters(), "CircuitBreaker");
        assertThat(cbFilter).isNotNull();
        assertThat(cbFilter.getArgs()).containsEntry("name", "user-service-cb");
        assertThat(cbFilter.getArgs()).containsEntry("fallbackUri", "forward:/fallback/user");
        
        // Verify Retry filter
        FilterDefinition retryFilter = findFilterByName(userRoute.getFilters(), "Retry");
        assertThat(retryFilter).isNotNull();
        assertThat(retryFilter.getArgs()).containsEntry("retries", "3");
        
        // Verify metadata
        Map<String, Object> metadata = userRoute.getMetadata();
        assertThat(metadata).containsEntry("timeout", "5000");
        assertThat(metadata).containsEntry("enabled", "true");
        assertThat(metadata).containsEntry("order", "1");
    }

    @Test
    void testProductServiceRouteEquivalence() {
        // Verify product service route configuration matches expected YAML equivalent
        RouteDefinition productRoute = findRouteById("product-service-routes");
        assertThat(productRoute).isNotNull();
        
        // Verify URI
        assertThat(productRoute.getUri().toString()).isEqualTo("http://localhost:8082");
        
        // Verify predicates
        assertThat(productRoute.getPredicates()).hasSize(1);
        PredicateDefinition pathPredicate = productRoute.getPredicates().get(0);
        assertThat(pathPredicate.getName()).isEqualTo("Path");
        assertThat(pathPredicate.getArgs().values()).contains("/product/**");
        
        // Verify filters
        assertThat(productRoute.getFilters()).hasSize(2);
        
        // Verify CircuitBreaker filter
        FilterDefinition cbFilter = findFilterByName(productRoute.getFilters(), "CircuitBreaker");
        assertThat(cbFilter).isNotNull();
        assertThat(cbFilter.getArgs()).containsEntry("name", "product-service-cb");
        assertThat(cbFilter.getArgs()).containsEntry("fallbackUri", "forward:/fallback/product");
        
        // Verify metadata
        Map<String, Object> metadata = productRoute.getMetadata();
        assertThat(metadata).containsEntry("timeout", "5000");
        assertThat(metadata).containsEntry("enabled", "true");
        assertThat(metadata).containsEntry("order", "2");
    }

    @Test
    void testCircuitBreakerConfigurationEquivalence() {
        // Verify circuit breaker configurations match expected YAML equivalent
        
        // Test user service circuit breaker
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        assertThat(userCb).isNotNull();
        
        CircuitBreakerConfig userConfig = userCb.getCircuitBreakerConfig();
        assertThat(userConfig.getSlidingWindowSize()).isEqualTo(10);
        assertThat(userConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(userConfig.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(userConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(userConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        
        // Test product service circuit breaker
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        assertThat(productCb).isNotNull();
        
        CircuitBreakerConfig productConfig = productCb.getCircuitBreakerConfig();
        assertThat(productConfig.getSlidingWindowSize()).isEqualTo(10);
        assertThat(productConfig.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(productConfig.getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(productConfig.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        assertThat(productConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
    }

    @Test
    void testTimeLimiterConfigurationEquivalence() {
        // Verify time limiter configurations match expected YAML equivalent
        
        // Test user service time limiter
        TimeLimiter userTl = timeLimiterRegistry.timeLimiter("user-service-cb");
        assertThat(userTl).isNotNull();
        assertThat(userTl.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
        
        // Test product service time limiter
        TimeLimiter productTl = timeLimiterRegistry.timeLimiter("product-service-cb");
        assertThat(productTl).isNotNull();
        assertThat(productTl.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void testManagementEndpointsEquivalence() {
        // Verify management endpoints configuration matches expected YAML equivalent
        WebEndpointProperties.Exposure exposure = webEndpointProperties.getExposure();
        assertThat(exposure.getInclude()).contains(
            "health", "info", "circuitbreakers", "circuitbreakerevents", "refresh"
        );
    }

    @Test
    void testRetryFilterConfigurationEquivalence() {
        // Verify retry filter configurations match expected YAML equivalent
        RouteDefinition userRoute = findRouteById("user-service-routes");
        FilterDefinition retryFilter = findFilterByName(userRoute.getFilters(), "Retry");
        
        assertThat(retryFilter).isNotNull();
        assertThat(retryFilter.getArgs()).containsEntry("retries", "3");
        assertThat(retryFilter.getArgs()).containsKey("statuses");
        assertThat(retryFilter.getArgs()).containsKey("methods");
        
        // Verify backoff configuration
        assertThat(retryFilter.getArgs()).containsKey("backoff.firstBackoff");
        assertThat(retryFilter.getArgs()).containsKey("backoff.maxBackoff");
        assertThat(retryFilter.getArgs()).containsKey("backoff.factor");
        assertThat(retryFilter.getArgs()).containsKey("backoff.basedOnPreviousValue");
    }

    @Test
    void testCompleteConfigurationIntegrity() {
        // Verify that all expected routes are present
        assertThat(gatewayProperties.getRoutes()).hasSize(2);
        
        // Verify that all expected circuit breakers are present
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).hasSize(2);
        
        // Verify that all expected time limiters are present
        assertThat(timeLimiterRegistry.getAllTimeLimiters()).hasSize(2);
        
        // Verify that management endpoints are properly configured
        assertThat(webEndpointProperties.getExposure().getInclude()).isNotEmpty();
    }

    // Helper methods
    private RouteDefinition findRouteById(String id) {
        return gatewayProperties.getRoutes().stream()
            .filter(route -> id.equals(route.getId()))
            .findFirst()
            .orElse(null);
    }

    private FilterDefinition findFilterByName(List<FilterDefinition> filters, String name) {
        return filters.stream()
            .filter(filter -> name.equals(filter.getName()))
            .findFirst()
            .orElse(null);
    }
}