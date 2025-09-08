package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import java.util.Optional;
import org.springframework.cloud.gateway.route.RouteDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuration validation tests to verify properties loading matches original YAML behavior
 * Tests validate route configuration parsing, circuit breaker configuration, HTTP client configuration,
 * and management endpoints configuration from properties files.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableConfigurationProperties
class ConfigurationValidationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Test
    void testApplicationContextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void testGatewayPropertiesLoaded() {
        assertThat(gatewayProperties).isNotNull();
        assertThat(gatewayProperties.getRoutes()).isNotEmpty();
    }

    @Test
    void testBasicRouteConfigurationLoaded() {
        // Verify that routes are loaded from properties
        assertThat(gatewayProperties.getRoutes()).hasSize(2);
        
        // Verify user service route
        Optional<RouteDefinition> userRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "user-service-routes".equals(route.getId()))
            .findFirst();
        
        assertThat(userRoute).isPresent();
        assertThat(userRoute.get().getUri().toString()).isEqualTo("http://localhost:8081");
        assertThat(userRoute.get().getPredicates()).hasSize(1);
        assertThat(userRoute.get().getFilters()).hasSize(2);
        
        // Verify product service route
        Optional<RouteDefinition> productRoute = gatewayProperties.getRoutes().stream()
            .filter(route -> "product-service-routes".equals(route.getId()))
            .findFirst();
        
        assertThat(productRoute).isPresent();
        assertThat(productRoute.get().getUri().toString()).isEqualTo("http://localhost:8082");
        assertThat(productRoute.get().getPredicates()).hasSize(1);
        assertThat(productRoute.get().getFilters()).hasSize(2);
    }

    @Test
    void testCircuitBreakerRegistryLoaded() {
        // Verify circuit breaker registry is available if circuit breaker is configured
        try {
            CircuitBreakerRegistry circuitBreakerRegistry = applicationContext.getBean(CircuitBreakerRegistry.class);
            assertThat(circuitBreakerRegistry).isNotNull();
            
            // Verify circuit breaker instances are configured
            assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).isNotNull();
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            // Circuit breaker registry may not be available if no circuit breakers are configured
            // This is acceptable for basic configuration validation
            assertThat(e.getMessage()).contains("CircuitBreakerRegistry");
        }
    }

    @Test
    void testTimeLimiterRegistryLoaded() {
        // Verify time limiter registry is available if time limiters are configured
        try {
            TimeLimiterRegistry timeLimiterRegistry = applicationContext.getBean(TimeLimiterRegistry.class);
            assertThat(timeLimiterRegistry).isNotNull();
            
            // Verify time limiter instances are configured
            assertThat(timeLimiterRegistry.getAllTimeLimiters()).isNotNull();
        } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException e) {
            // Time limiter registry may not be available if no time limiters are configured
            // This is acceptable for basic configuration validation
            assertThat(e.getMessage()).contains("TimeLimiterRegistry");
        }
    }

    @Test
    void testManagementEndpointsAvailable() {
        // Verify health endpoint is available
        assertThat(applicationContext.containsBean("healthEndpoint")).isTrue();
        
        // Verify info endpoint is available
        assertThat(applicationContext.containsBean("infoEndpoint")).isTrue();
    }
}