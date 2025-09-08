package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter for ensuring consistent response formatting across the gateway.
 * Adds standard headers, ensures proper content types, and maintains response consistency.
 * Handles CORS headers and security headers for all responses.
 */
@Component
public class ResponseFormattingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFormattingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> formatResponse(exchange))
                .doOnError(throwable -> formatErrorResponse(exchange));
    }

    private void formatResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        
        // Add standard headers
        addStandardHeaders(headers);
        
        // Ensure proper content type for JSON responses
        ensureContentType(headers, response);
        
        // Add CORS headers if needed
        addCorsHeaders(headers, exchange);
        
        logger.debug("Response formatted for {}: {} with headers: {}", 
                exchange.getRequest().getURI().getPath(),
                response.getStatusCode(),
                headers.keySet());
    }

    private void formatErrorResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        
        // Add standard headers for error responses
        addStandardHeaders(headers);
        
        // Ensure JSON content type for errors
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        
        // Add CORS headers for error responses
        addCorsHeaders(headers, exchange);
        
        logger.debug("Error response formatted for {}: {}", 
                exchange.getRequest().getURI().getPath(),
                response.getStatusCode());
    }

    private void addStandardHeaders(HttpHeaders headers) {
        // Add cache control headers
        if (!headers.containsKey(HttpHeaders.CACHE_CONTROL)) {
            headers.setCacheControl("no-cache, no-store, must-revalidate");
        }
        
        // Add security headers
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add("X-Frame-Options", "DENY");
        headers.add("X-XSS-Protection", "1; mode=block");
        
        // Add gateway identification header
        headers.add("X-Gateway-Version", "1.0");
        headers.add("X-Response-Time", String.valueOf(System.currentTimeMillis()));
    }

    private void ensureContentType(HttpHeaders headers, ServerHttpResponse response) {
        // If no content type is set and response has body, default to JSON
        if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            // Check if response likely contains JSON based on status
            if (isJsonResponse(response)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }
    }

    private boolean isJsonResponse(ServerHttpResponse response) {
        // Most API responses should be JSON
        return response.getStatusCode() != null && 
               (response.getStatusCode().is2xxSuccessful() || 
                response.getStatusCode().is4xxClientError() || 
                response.getStatusCode().is5xxServerError());
    }

    private void addCorsHeaders(HttpHeaders headers, ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);
        
        // Add CORS headers for cross-origin requests
        if (origin != null) {
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, 
                    "Content-Type, Authorization, X-Requested-With");
            headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }
    }

    @Override
    public int getOrder() {
        // Execute after most filters but before error handling
        return Ordered.LOWEST_PRECEDENCE - 10;
    }
}