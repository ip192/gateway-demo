package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for dynamic routing functionality with circuit breaker and retry support.
 * The DynamicRouteLocator is automatically registered as a @Component
 * and will be picked up by Spring Cloud Gateway.
 */
@Configuration
public class DynamicRoutingConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicRoutingConfiguration.class);
    
    public DynamicRoutingConfiguration() {
        logger.info("Dynamic routing configuration initialized with circuit breaker and retry support");
    }
}