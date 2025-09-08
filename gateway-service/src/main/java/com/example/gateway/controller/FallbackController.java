package com.example.gateway.controller;

import com.example.gateway.model.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Fallback controller to provide circuit breaker fallback responses
 */
@RestController
public class FallbackController {

    private static final Logger logger = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/user")
    public Mono<ResponseEntity<ApiResponse<Object>>> userServiceFallback(ServerWebExchange exchange) {
        logger.warn("User service fallback triggered for request: {}", exchange.getRequest().getPath());
        
        ApiResponse<Object> response = new ApiResponse<Object>();
        response.setSuccess(false);
        response.setMessage("User service is temporarily unavailable. Please try again later.");
        response.setData(null);
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/fallback/product")
    public Mono<ResponseEntity<ApiResponse<Object>>> productServiceFallback(ServerWebExchange exchange) {
        logger.warn("Product service fallback triggered for request: {}", exchange.getRequest().getPath());
        
        ApiResponse<Object> response = new ApiResponse<Object>();
        response.setSuccess(false);
        response.setMessage("Product service is temporarily unavailable. Please try again later.");
        response.setData(null);
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/fallback/general")
    public Mono<ResponseEntity<ApiResponse<Object>>> generalFallback(ServerWebExchange exchange) {
        logger.warn("General fallback triggered for request: {}", exchange.getRequest().getPath());
        
        ApiResponse<Object> response = new ApiResponse<Object>();
        response.setSuccess(false);
        response.setMessage("Service is temporarily unavailable. Please try again later.");
        response.setData(null);
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    @RequestMapping("/fallback/timeout")
    public Mono<ResponseEntity<ApiResponse<Object>>> timeoutFallback(ServerWebExchange exchange) {
        logger.warn("Timeout fallback triggered for request: {}", exchange.getRequest().getPath());
        
        ApiResponse<Object> response = new ApiResponse<Object>();
        response.setSuccess(false);
        response.setMessage("Request timeout. The service took too long to respond.");
        response.setData(null);
        
        return Mono.just(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(response));
    }
}