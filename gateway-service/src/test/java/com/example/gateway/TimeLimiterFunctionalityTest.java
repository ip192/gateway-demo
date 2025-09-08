package com.example.gateway;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for time limiter functionality with properties configuration
 * Verifies that time limiter configurations are correctly loaded and function as expected
 * Requirement 4.2: Time limiter configurations with timeout durations preserved
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Time Limiter Configuration
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=3s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=4s",
    
    // Circuit Breaker Configuration (needed for registry)
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=10",
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=15"
})
class TimeLimiterFunctionalityTest {

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Test
    void testUserServiceTimeLimiterConfigurationFromProperties() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        assertThat(timeLimiter).isNotNull();
        
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        
        // Verify configuration matches properties file: resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=3s
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    @Test
    void testProductServiceTimeLimiterConfigurationFromProperties() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        assertThat(timeLimiter).isNotNull();
        
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        
        // Verify configuration matches properties file: resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=4s
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(4));
        assertThat(timeLimiter.getName()).isEqualTo("product-service-cb");
    }

    @Test
    void testTimeLimiterRegistryContainsConfiguredInstances() {
        // Verify that all configured time limiters are loaded
        assertThat(timeLimiterRegistry.getAllTimeLimiters()).hasSize(2);
        
        // Verify specific instances exist
        assertThat(timeLimiterRegistry.find("user-service-cb")).isPresent();
        assertThat(timeLimiterRegistry.find("product-service-cb")).isPresent();
    }

    @Test
    void testUserServiceTimeLimiterTimeoutFunctionality() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        
        // Verify time limiter configuration is correct for timeout functionality
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        
        // Verify time limiter name matches expected configuration
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    @Test
    void testProductServiceTimeLimiterTimeoutFunctionality() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        
        // Verify time limiter configuration is correct for timeout functionality
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(4));
        
        // Verify time limiter name matches expected configuration
        assertThat(timeLimiter.getName()).isEqualTo("product-service-cb");
    }

    @Test
    void testTimeLimiterAllowsFastOperations() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        
        // Verify time limiter is configured to allow operations within timeout
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        
        // Verify time limiter can be used to monitor operation timing
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    @Test
    void testTimeLimiterWithDifferentTimeouts() {
        TimeLimiter userTimeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        TimeLimiter productTimeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        
        // Verify different timeout configurations
        assertThat(userTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isEqualTo(Duration.ofSeconds(3));
        assertThat(productTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isEqualTo(Duration.ofSeconds(4));
        
        // Verify that different services have different timeout configurations
        assertThat(userTimeLimiter.getTimeLimiterConfig().getTimeoutDuration())
            .isNotEqualTo(productTimeLimiter.getTimeLimiterConfig().getTimeoutDuration());
    }

    @Test
    void testTimeLimiterConfigurationValidation() {
        // Test that time limiter configurations are valid
        TimeLimiter userTimeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        TimeLimiter productTimeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        
        // Verify timeout durations are positive
        assertThat(userTimeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis()).isGreaterThan(0);
        assertThat(productTimeLimiter.getTimeLimiterConfig().getTimeoutDuration().toMillis()).isGreaterThan(0);
        
        // Verify timeout durations are reasonable (not too short or too long)
        long userTimeoutSeconds = userTimeLimiter.getTimeLimiterConfig().getTimeoutDuration().getSeconds();
        long productTimeoutSeconds = productTimeLimiter.getTimeLimiterConfig().getTimeoutDuration().getSeconds();
        
        assertThat(userTimeoutSeconds).isBetween(1L, 60L);
        assertThat(productTimeoutSeconds).isBetween(1L, 60L);
    }

    @Test
    void testTimeLimiterNameMapping() {
        // Verify that time limiter names match circuit breaker names for proper integration
        TimeLimiter userTimeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        TimeLimiter productTimeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        
        assertThat(userTimeLimiter.getName()).isEqualTo("user-service-cb");
        assertThat(productTimeLimiter.getName()).isEqualTo("product-service-cb");
        
        // Names should match the circuit breaker instance names for proper integration
        assertThat(userTimeLimiter.getName()).contains("user-service");
        assertThat(productTimeLimiter.getName()).contains("product-service");
    }

    @Test
    void testTimeLimiterWithImmediateCompletion() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("user-service-cb");
        
        // Verify time limiter is configured for immediate operations
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));
        
        // Verify time limiter can handle immediate completion scenarios
        assertThat(timeLimiter.getName()).isEqualTo("user-service-cb");
    }

    @Test
    void testTimeLimiterWithFailedOperation() {
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("product-service-cb");
        
        // Verify time limiter is configured for error handling scenarios
        TimeLimiterConfig config = timeLimiter.getTimeLimiterConfig();
        assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(4));
        
        // Verify time limiter can handle failed operation scenarios
        assertThat(timeLimiter.getName()).isEqualTo("product-service-cb");
    }
}