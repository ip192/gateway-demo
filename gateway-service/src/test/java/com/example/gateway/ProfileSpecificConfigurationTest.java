package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.time.Duration;
import java.util.Optional;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for profile-specific configuration validation
 * Tests different Spring profiles to ensure profile-specific properties are loaded correctly
 */
class ProfileSpecificConfigurationTest {

    /**
     * Test circuit-breaker profile configuration
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("circuit-breaker")
    static class CircuitBreakerProfileTest {

        @Autowired
        private GatewayProperties gatewayProperties;

        @Autowired
        private CircuitBreakerRegistry circuitBreakerRegistry;

        @Autowired
        private TimeLimiterRegistry timeLimiterRegistry;

        @Autowired
        private WebEndpointProperties webEndpointProperties;

        @Test
        void testCircuitBreakerProfileRoutes() {
            // Verify circuit-breaker profile routes are loaded
            assertThat(gatewayProperties.getRoutes()).isNotEmpty();
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-with-cb".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
        }

        @Test
        void testEnhancedCircuitBreakerConfiguration() {
            // Verify enhanced circuit breaker configurations from circuit-breaker profile
            CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
            assertThat(productCb).isNotNull();
            
            CircuitBreakerConfig config = productCb.getCircuitBreakerConfig();
            // Circuit breaker profile has different settings for product service
            assertThat(config.getSlidingWindowSize()).isEqualTo(15);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(8);
            assertThat(config.getFailureRateThreshold()).isEqualTo(55.0f);
            assertThat(config.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(12));
        }

        @Test
        void testCircuitBreakerSpecificEndpoints() {
            // Verify circuit breaker specific management endpoints
            WebEndpointProperties.Exposure exposure = webEndpointProperties.getExposure();
            assertThat(exposure.getInclude()).contains("circuitbreakers", "circuitbreakerevents");
        }

        @Test
        void testTimeLimiterProfileConfiguration() {
            // Verify time limiter configurations in circuit-breaker profile
            TimeLimiter userTl = timeLimiterRegistry.timeLimiter("user-service-cb");
            assertThat(userTl).isNotNull();
            assertThat(userTl.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
            
            TimeLimiter productTl = timeLimiterRegistry.timeLimiter("product-service-cb");
            assertThat(productTl).isNotNull();
            assertThat(productTl.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(4));
        }
    }

    /**
     * Test performance profile configuration
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("performance")
    static class PerformanceProfileTest {

        @Autowired
        private GatewayProperties gatewayProperties;

        @Autowired
        private HttpClientProperties httpClientProperties;

        @Autowired
        private WebEndpointProperties webEndpointProperties;

        @Test
        void testPerformanceProfileRoutes() {
            // Verify performance profile routes are loaded
            assertThat(gatewayProperties.getRoutes()).isNotEmpty();
            
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
        }

        @Test
        void testPerformanceHttpClientConfiguration() {
            // Verify performance-optimized HTTP client settings
            assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(5000);
            assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(30));
            
            HttpClientProperties.Pool pool = httpClientProperties.getPool();
            assertThat(pool.getMaxConnections()).isEqualTo(500);
            assertThat(pool.getMaxIdleTime()).isEqualTo(Duration.ofSeconds(30));
        }

        @Test
        void testPerformanceEndpointsConfiguration() {
            // Verify performance monitoring endpoints
            WebEndpointProperties.Exposure exposure = webEndpointProperties.getExposure();
            assertThat(exposure.getInclude()).contains("metrics", "prometheus", "performance");
        }

        @Test
        void testOptimizedRetryConfiguration() {
            // Verify optimized retry settings in performance profile
            Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-optimized".equals(route.getId()))
                .findFirst();
            
            assertThat(userRoute).isPresent();
            
            Optional<FilterDefinition> retryFilter = userRoute.get().getFilters().stream()
                .filter(filter -> "Retry".equals(filter.getName()))
                .findFirst();
            
            assertThat(retryFilter).isPresent();
            // Performance profile uses fewer retries for faster response
            assertThat(retryFilter.get().getArgs()).containsEntry("retries", "2");
        }
    }

    /**
     * Test dynamic-routing profile configuration
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dynamic-routing")
    static class DynamicRoutingProfileTest {

        @Autowired
        private GatewayProperties gatewayProperties;

        @Test
        void testDynamicRoutingProfileRoutes() {
            // Verify dynamic routing profile has multiple route formats
            assertThat(gatewayProperties.getRoutes()).isNotEmpty();
            
            // Should have both standard and example routes
            Optional<RouteDefinition> standardRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-standard".equals(route.getId()))
                .findFirst();
            
            assertThat(standardRoute).isPresent();
        }

        @Test
        void testComplexRouteConfiguration() {
            // Verify complex route with multiple predicates and filters
            Optional<RouteDefinition> complexRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "complex-route-example".equals(route.getId()))
                .findFirst();
            
            assertThat(complexRoute).isPresent();
            assertThat(complexRoute.get().getPredicates()).hasSize(3); // Path, Method, Header
            assertThat(complexRoute.get().getFilters()).hasSize(3); // AddRequestHeader, AddResponseHeader, CircuitBreaker
        }

        @Test
        void testWeightBasedRouting() {
            // Verify weight-based load balancing routes
            Optional<RouteDefinition> weightedRoute1 = gatewayProperties.getRoutes().stream()
                .filter(route -> "weighted-route-example-1".equals(route.getId()))
                .findFirst();
            
            Optional<RouteDefinition> weightedRoute2 = gatewayProperties.getRoutes().stream()
                .filter(route -> "weighted-route-example-2".equals(route.getId()))
                .findFirst();
            
            assertThat(weightedRoute1).isPresent();
            assertThat(weightedRoute2).isPresent();
            
            // Both should have Weight predicates
            assertThat(weightedRoute1.get().getPredicates()).anyMatch(
                predicate -> predicate.getName().equals("Weight")
            );
            assertThat(weightedRoute2.get().getPredicates()).anyMatch(
                predicate -> predicate.getName().equals("Weight")
            );
        }

        @Test
        void testRewritePathFilters() {
            // Verify RewritePath filters in standard format routes
            Optional<RouteDefinition> standardRoute = gatewayProperties.getRoutes().stream()
                .filter(route -> "user-service-standard".equals(route.getId()))
                .findFirst();
            
            assertThat(standardRoute).isPresent();
            
            Optional<FilterDefinition> rewriteFilter = standardRoute.get().getFilters().stream()
                .filter(filter -> "RewritePath".equals(filter.getName()))
                .findFirst();
            
            assertThat(rewriteFilter).isPresent();
            assertThat(rewriteFilter.get().getArgs()).containsKey("regexp");
            assertThat(rewriteFilter.get().getArgs()).containsKey("replacement");
        }
    }
}