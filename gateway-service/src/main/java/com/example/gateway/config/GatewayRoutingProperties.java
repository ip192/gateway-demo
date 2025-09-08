package com.example.gateway.config;

import com.example.gateway.model.CircuitBreakerConfig;
import com.example.gateway.model.RetryConfig;
import com.example.gateway.model.RouteConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for gateway routing
 * Maps to gateway.* properties in application configuration
 * Supports hot reload via @RefreshScope annotation
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "gateway")
public class GatewayRoutingProperties {
    
    private List<RouteConfig> routes;
    private CircuitBreakerConfig circuitBreaker;
    private RetryConfig retry;

    public GatewayRoutingProperties() {}

    public List<RouteConfig> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteConfig> routes) {
        this.routes = routes;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    @Override
    public String toString() {
        return "GatewayRoutingProperties{" +
                "routes=" + routes +
                ", circuitBreaker=" + circuitBreaker +
                ", retry=" + retry +
                '}';
    }
}