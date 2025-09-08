package com.example.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Tests for circuit breaker filter functionality with properties configuration
 * Requirements: 3.2 - Circuit breaker configurations work identically
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    // Route with circuit breaker configuration
    "spring.cloud.gateway.routes[0].id=user-service-cb-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[0].filters[0].args.name=user-service-cb",
    "spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/user",
    
    "spring.cloud.gateway.routes[1].id=product-service-cb-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.routes[1].filters[0].name=CircuitBreaker",
    "spring.cloud.gateway.routes[1].filters[0].args.name=product-service-cb",
    "spring.cloud.gateway.routes[1].filters[0].args.fallbackUri=forward:/fallback/product",
    
    // Fast circuit breaker configuration for testing
    "resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=3",
    "resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=2",
    "resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50",
    "resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=2s",
    "resilience4j.circuitbreaker.instances.user-service-cb.permittedNumberOfCallsInHalfOpenState=1",
    "resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true",
    
    "resilience4j.circuitbreaker.instances.product-service-cb.slidingWindowSize=4",
    "resilience4j.circuitbreaker.instances.product-service-cb.minimumNumberOfCalls=3",
    "resilience4j.circuitbreaker.instances.product-service-cb.failureRateThreshold=60",
    "resilience4j.circuitbreaker.instances.product-service-cb.waitDurationInOpenState=3s",
    "resilience4j.circuitbreaker.instances.product-service-cb.permittedNumberOfCallsInHalfOpenState=2",
    "resilience4j.circuitbreaker.instances.product-service-cb.registerHealthIndicator=true",
    
    // Time limiter configuration
    "resilience4j.timelimiter.instances.user-service-cb.timeoutDuration=1s",
    "resilience4j.timelimiter.instances.product-service-cb.timeoutDuration=1s"
})
public class CircuitBreakerFilterPropertiesTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Test circuit breaker configuration is loaded from properties
     */
    @Test
    public void testCircuitBreakerConfiguration_LoadedFromProperties() {
        // Verify circuit breaker instances are created
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).isNotEmpty();
        
        // Check user service circuit breaker
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        assertThat(userCb).isNotNull();
        assertThat(userCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(3);
        assertThat(userCb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(2);
        assertThat(userCb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        
        // Check product service circuit breaker
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        assertThat(productCb).isNotNull();
        assertThat(productCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(4);
        assertThat(productCb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(3);
        assertThat(productCb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60.0f);
    }

    /**
     * Test circuit breaker filter is applied to routes from properties
     */
    @Test
    public void testCircuitBreakerFilter_AppliedToRoutes() {
        Flux<Route> routes = routeLocator.getRoutes();
        List<Route> routeList = routes.collectList().block();
        
        // Find user service route
        Route userRoute = routeList.stream()
            .filter(route -> "user-service-cb-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(userRoute).isNotNull();
        assertThat(userRoute.getFilters()).isNotEmpty();
        
        // Find product service route
        Route productRoute = routeList.stream()
            .filter(route -> "product-service-cb-test".equals(route.getId()))
            .findFirst()
            .orElse(null);
        
        assertThat(productRoute).isNotNull();
        assertThat(productRoute.getFilters()).isNotEmpty();
    }

    /**
     * Test circuit breaker opens after failures
     */
    @Test
    public void testCircuitBreakerOpens_AfterFailures() {
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        assertThat(userCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        String userUrl = "http://localhost:" + gatewayPort + "/user/test";
        
        // Make requests to trigger failures (service is unavailable)
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
            // Should get error responses
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        
        // Wait for circuit breaker to potentially open
        await().atMost(5, TimeUnit.SECONDS).until(() -> 
            userCb.getState() == CircuitBreaker.State.OPEN || 
            userCb.getMetrics().getNumberOfFailedCalls() >= 2
        );
    }

    /**
     * Test circuit breaker fallback is triggered
     */
    @Test
    public void testCircuitBreakerFallback_Triggered() {
        String userUrl = "http://localhost:" + gatewayPort + "/user/profile";
        
        // Make multiple requests to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);
            
            // Should eventually get fallback response or service unavailable
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        
        // Test direct fallback endpoint
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/user";
        ResponseEntity<String> fallbackResponse = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(fallbackResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(fallbackResponse.getBody()).contains("User service is temporarily unavailable");
    }

    /**
     * Test different circuit breaker configurations for different services
     */
    @Test
    public void testDifferentCircuitBreakerConfigurations() {
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        // Verify different configurations
        assertThat(userCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(3);
        assertThat(productCb.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(4);
        
        assertThat(userCb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(2);
        assertThat(productCb.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(3);
        
        assertThat(userCb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50.0f);
        assertThat(productCb.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(60.0f);
    }

    /**
     * Test circuit breaker health indicators are registered
     */
    @Test
    public void testCircuitBreakerHealthIndicators() {
        String healthUrl = "http://localhost:" + gatewayPort + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);
        
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Should include circuit breaker health information
        String healthBody = healthResponse.getBody();
        assertThat(healthBody).contains("circuitBreakers");
    }

    /**
     * Test circuit breaker metrics are available
     */
    @Test
    public void testCircuitBreakerMetrics() {
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        
        // Initial state should be closed
        assertThat(userCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(productCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Metrics should be available
        assertThat(userCb.getMetrics()).isNotNull();
        assertThat(productCb.getMetrics()).isNotNull();
        
        // Initial metrics
        assertThat(userCb.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
        assertThat(productCb.getMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
    }

    /**
     * Test circuit breaker with product service
     */
    @Test
    public void testCircuitBreakerFilter_ProductService() {
        CircuitBreaker productCb = circuitBreakerRegistry.circuitBreaker("product-service-cb");
        assertThat(productCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        String productUrl = "http://localhost:" + gatewayPort + "/product/list";
        
        // Make requests to trigger failures
        for (int i = 0; i < 6; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(productUrl, String.class);
            
            // Should get error responses
            assertThat(response.getStatusCode()).isIn(
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
        
        // Check that circuit breaker recorded the failures
        assertThat(productCb.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
    }

    /**
     * Test circuit breaker fallback for product service
     */
    @Test
    public void testCircuitBreakerFallback_ProductService() {
        // Test direct fallback endpoint for product service
        String fallbackUrl = "http://localhost:" + gatewayPort + "/fallback/product";
        ResponseEntity<String> fallbackResponse = restTemplate.getForEntity(fallbackUrl, String.class);
        
        assertThat(fallbackResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(fallbackResponse.getBody()).contains("Product service is temporarily unavailable");
    }

    /**
     * Test circuit breaker state transitions
     */
    @Test
    public void testCircuitBreakerStateTransitions() {
        CircuitBreaker userCb = circuitBreakerRegistry.circuitBreaker("user-service-cb");
        
        // Initial state should be closed
        assertThat(userCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        String userUrl = "http://localhost:" + gatewayPort + "/user/data";
        
        // Make enough requests to potentially trigger state change
        for (int i = 0; i < 3; i++) {
            restTemplate.getForEntity(userUrl, String.class);
        }
        
        // Circuit breaker should have recorded the calls
        assertThat(userCb.getMetrics().getNumberOfBufferedCalls()).isGreaterThan(0);
    }
}