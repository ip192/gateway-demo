package com.example.gateway.controller;

import com.example.gateway.model.ApiResponse;
import com.example.gateway.service.RouteRefreshService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/gateway")
public class GatewayController {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);
    
    private final RouteRefreshService routeRefreshService;
    
    public GatewayController(RouteRefreshService routeRefreshService) {
        this.routeRefreshService = routeRefreshService;
    }

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("Gateway is running", "success");
    }

    @GetMapping("/info")
    public ApiResponse info() {
        int routeCount = routeRefreshService.getCurrentRouteCount();
        return new ApiResponse("Gateway Service - Dynamic Routing Enabled. Active routes: " + routeCount, "success");
    }
    
    @PostMapping("/refresh-routes")
    public Mono<ApiResponse> refreshRoutes() {
        logger.info("Manual route refresh requested via REST endpoint");
        
        return routeRefreshService.refreshRoutes()
                .then(Mono.fromCallable(() -> {
                    int routeCount = routeRefreshService.getCurrentRouteCount();
                    return new ApiResponse("Routes refreshed successfully. Active routes: " + routeCount, "success");
                }))
                .onErrorReturn(new ApiResponse("Failed to refresh routes", "error"));
    }
    
    @GetMapping("/routes/{routeId}/status")
    public ApiResponse getRouteStatus(@PathVariable String routeId) {
        boolean enabled = routeRefreshService.isRouteEnabled(routeId);
        String status = enabled ? "enabled" : "disabled";
        return new ApiResponse("Route " + routeId + " is " + status, "success");
    }
}