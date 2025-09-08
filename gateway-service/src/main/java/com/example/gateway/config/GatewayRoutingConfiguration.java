package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configuration class that validates gateway routing properties on application startup
 */
@Configuration
public class GatewayRoutingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(GatewayRoutingConfiguration.class);

    private final GatewayRoutingProperties gatewayRoutingProperties;
    private final RouteConfigValidator routeConfigValidator;

    @Autowired
    public GatewayRoutingConfiguration(GatewayRoutingProperties gatewayRoutingProperties,
                                     RouteConfigValidator routeConfigValidator) {
        this.gatewayRoutingProperties = gatewayRoutingProperties;
        this.routeConfigValidator = routeConfigValidator;
    }

    /**
     * Validates route configuration after application context is fully loaded
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Validating gateway routing configuration...");
        
        try {
            if (gatewayRoutingProperties.getRoutes() != null && 
                !gatewayRoutingProperties.getRoutes().isEmpty()) {
                
                routeConfigValidator.validateRoutes(gatewayRoutingProperties.getRoutes());
                logger.info("Gateway routing configuration validation successful. " +
                           "Loaded {} routes.", gatewayRoutingProperties.getRoutes().size());
                
                // Log route details for debugging
                gatewayRoutingProperties.getRoutes().forEach(route -> 
                    logger.debug("Route configured: {} -> {}", route.getId(), route.getUri()));
                    
            } else {
                logger.warn("No routes configured in gateway.routes property");
            }
        } catch (IllegalArgumentException e) {
            logger.error("Gateway routing configuration validation failed: {}", e.getMessage());
            throw new IllegalStateException("Invalid gateway routing configuration", e);
        }
    }
}