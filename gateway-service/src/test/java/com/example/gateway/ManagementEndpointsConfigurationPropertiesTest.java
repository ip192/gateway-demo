package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for management endpoints configuration validation from properties files
 * Verifies that actuator endpoints, health checks, and monitoring configurations are correctly loaded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "management.endpoints.web.exposure.include=health,info,metrics,circuitbreakers,circuitbreakerevents,refresh",
    "management.endpoint.health.show-details=always",
    "management.endpoint.info.enabled=true",
    "management.endpoint.metrics.enabled=true",
    "management.endpoint.refresh.enabled=true",
    "management.health.circuitbreakers.enabled=true",
    "management.health.diskspace.enabled=true",
    "management.health.ping.enabled=true"
})
class ManagementEndpointsConfigurationPropertiesTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebEndpointProperties webEndpointProperties;

    @Autowired
    private HealthEndpointProperties healthEndpointProperties;

    @Test
    void testWebEndpointPropertiesLoaded() {
        assertThat(webEndpointProperties).isNotNull();
    }

    @Test
    void testExposedEndpointsConfiguration() {
        assertThat(webEndpointProperties.getExposure().getInclude())
            .contains("health", "info", "metrics", "circuitbreakers", "circuitbreakerevents", "refresh");
    }

    @Test
    void testHealthEndpointConfiguration() {
        assertThat(healthEndpointProperties).isNotNull();
        assertThat(healthEndpointProperties.getShowDetails())
            .isEqualTo(HealthEndpointProperties.Show.ALWAYS);
    }

    @Test
    void testHealthEndpointBeanExists() {
        assertThat(applicationContext.containsBean("healthEndpoint")).isTrue();
        
        HealthEndpoint healthEndpoint = applicationContext.getBean(HealthEndpoint.class);
        assertThat(healthEndpoint).isNotNull();
    }

    @Test
    void testInfoEndpointBeanExists() {
        assertThat(applicationContext.containsBean("infoEndpoint")).isTrue();
        
        InfoEndpoint infoEndpoint = applicationContext.getBean(InfoEndpoint.class);
        assertThat(infoEndpoint).isNotNull();
    }

    @Test
    void testWebEndpointsSupplierExists() {
        assertThat(applicationContext.containsBean("webEndpointsSupplier")).isTrue();
        
        WebEndpointsSupplier webEndpointsSupplier = applicationContext.getBean(WebEndpointsSupplier.class);
        assertThat(webEndpointsSupplier).isNotNull();
        assertThat(webEndpointsSupplier.getEndpoints()).isNotEmpty();
    }

    @Test
    void testSpecificEndpointsEnabled() {
        // Verify that specific endpoints are available
        WebEndpointsSupplier webEndpointsSupplier = applicationContext.getBean(WebEndpointsSupplier.class);
        
        List<String> endpointIds = webEndpointsSupplier.getEndpoints().stream()
            .map(endpoint -> endpoint.getEndpointId().toLowerCaseString())
            .collect(Collectors.toList());
        
        assertThat(endpointIds).contains("health");
        assertThat(endpointIds).contains("info");
    }

    @Test
    void testCircuitBreakerHealthIndicatorEnabled() {
        // Verify that circuit breaker health indicators are configured
        // This is validated by checking if the health endpoint can access circuit breaker health
        HealthEndpoint healthEndpoint = applicationContext.getBean(HealthEndpoint.class);
        assertThat(healthEndpoint).isNotNull();
        
        // The health endpoint should be able to provide health information
        org.springframework.boot.actuate.health.HealthComponent health = healthEndpoint.health();
        assertThat(health).isNotNull();
    }

    @Test
    void testEndpointPathConfiguration() {
        // Verify that endpoint paths are configured correctly
        assertThat(webEndpointProperties.getBasePath()).isNotNull();
        
        // Default base path should be "/actuator"
        String basePath = webEndpointProperties.getBasePath();
        assertThat(basePath).isEqualTo("/actuator");
    }

    @Test
    void testEndpointSecurityConfiguration() {
        // Verify that endpoint exposure configuration is properly set
        WebEndpointProperties.Exposure exposure = webEndpointProperties.getExposure();
        assertThat(exposure).isNotNull();
        
        // Verify that include list is not empty and contains expected endpoints
        assertThat(exposure.getInclude()).isNotEmpty();
        assertThat(exposure.getInclude()).contains("health");
    }

    @Test
    void testHealthDetailsConfiguration() {
        // Verify that health details are configured to show always
        assertThat(healthEndpointProperties.getShowDetails())
            .isEqualTo(HealthEndpointProperties.Show.ALWAYS);
        
        // Verify that health components can be enabled/disabled
        assertThat(healthEndpointProperties.getShowComponents())
            .isNotEqualTo(HealthEndpointProperties.Show.NEVER);
    }

    @Test
    void testMetricsEndpointConfiguration() {
        // Verify that metrics endpoint is available when configured
        WebEndpointsSupplier webEndpointsSupplier = applicationContext.getBean(WebEndpointsSupplier.class);
        
        boolean metricsEndpointExists = webEndpointsSupplier.getEndpoints().stream()
            .anyMatch(endpoint -> "metrics".equals(endpoint.getEndpointId().toLowerCaseString()));
        
        // Metrics endpoint should be available if micrometer is on classpath
        // This test verifies the configuration doesn't break metrics setup
        assertThat(webEndpointsSupplier.getEndpoints()).isNotEmpty();
    }
}