package com.example.gateway.filter;

import com.example.gateway.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global filter for unified error handling across the gateway.
 * Catches exceptions and converts them to standardized error responses.
 * Provides consistent error format for all downstream service failures.
 */
@Component
public class ErrorHandlingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> handleError(exchange, throwable));
    }

    private Mono<Void> handleError(ServerWebExchange exchange, Throwable throwable) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Determine error details based on exception type
        ErrorDetails errorDetails = determineErrorDetails(throwable);
        
        // Log the error
        logger.error("Gateway error occurred: {} - {}", 
                errorDetails.status, errorDetails.message, throwable);
        
        // Set response status and headers
        response.setStatusCode(errorDetails.status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        // Create standardized error response
        Map<String, Object> errorResponse = createErrorResponse(
                errorDetails.message, 
                errorDetails.status, 
                exchange.getRequest().getURI().getPath()
        );
        
        try {
            String responseJson = objectMapper.writeValueAsString(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(responseJson.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            logger.error("Failed to serialize error response", e);
            return response.setComplete();
        }
    }

    private ErrorDetails determineErrorDetails(Throwable throwable) {
        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) throwable;
            return new ErrorDetails(rse.getStatus(), rse.getReason());
        }
        
        if (throwable instanceof java.net.ConnectException) {
            return new ErrorDetails(HttpStatus.SERVICE_UNAVAILABLE, 
                    "Service temporarily unavailable");
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return new ErrorDetails(HttpStatus.GATEWAY_TIMEOUT, 
                    "Request timeout");
        }
        
        if (throwable.getMessage() != null && throwable.getMessage().contains("404")) {
            return new ErrorDetails(HttpStatus.NOT_FOUND, 
                    "Requested resource not found");
        }
        
        // Default to internal server error
        return new ErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred");
    }

    private Map<String, Object> createErrorResponse(String message, HttpStatus status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        errorResponse.put("status", status.value());
        errorResponse.put("statusText", status.getReasonPhrase());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", path);
        return errorResponse;
    }

    @Override
    public int getOrder() {
        // Execute late in the filter chain to catch all errors
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    private static class ErrorDetails {
        final HttpStatus status;
        final String message;

        ErrorDetails(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}