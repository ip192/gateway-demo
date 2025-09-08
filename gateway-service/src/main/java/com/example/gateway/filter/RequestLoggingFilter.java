package com.example.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Optimized global filter for logging requests and performance metrics.
 * Records request details, response status, and processing time with minimal overhead.
 * Implements Ordered to ensure proper filter execution order.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String START_TIME_ATTR = "startTime";
    private static final String REQUEST_PATH_ATTR = "requestPath";
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    private Timer requestTimer;
    
    public RequestLoggingFilter() {
        // Initialize timer if meter registry is available
        if (meterRegistry != null) {
            this.requestTimer = Timer.builder("gateway.request.processing.time")
                    .description("Time taken to process gateway requests")
                    .register(meterRegistry);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Record start time for performance measurement (use nanoTime for precision)
        long startTime = System.nanoTime();
        exchange.getAttributes().put(START_TIME_ATTR, startTime);
        exchange.getAttributes().put(REQUEST_PATH_ATTR, request.getPath().value());
        
        // Conditional logging based on log level to reduce overhead
        if (logger.isInfoEnabled()) {
            logRequest(request);
        }
        
        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logResponse(exchange, startTime))
                .doOnError(throwable -> logError(exchange, throwable, startTime));
    }

    private void logRequest(ServerHttpRequest request) {
        // Optimized logging - only log essential information
        logger.info("Request: {} {}", request.getMethod(), request.getPath().value());
        
        // Only log headers in debug mode to reduce overhead
        if (logger.isDebugEnabled()) {
            request.getHeaders().forEach((name, values) -> {
                if (!isSensitiveHeader(name)) {
                    logger.debug("Header: {} = {}", name, values);
                }
            });
        }
    }

    private void logResponse(ServerWebExchange exchange, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to milliseconds
        String requestPath = exchange.getAttribute(REQUEST_PATH_ATTR);
        
        // Record metrics if available
        if (requestTimer != null) {
            requestTimer.record(duration, TimeUnit.MILLISECONDS);
        }
        
        // Optimized logging
        if (logger.isInfoEnabled()) {
            logger.info("Response: {} -> {} ({}ms)", 
                    requestPath, 
                    response.getStatusCode(), 
                    duration);
        }
        
        // Performance warning for slow requests
        if (duration > 1000 && logger.isWarnEnabled()) {
            logger.warn("Slow request: {} took {}ms", requestPath, duration);
        }
        
        // Record performance metrics
        recordPerformanceMetrics(exchange, duration, false);
    }

    private void logError(ServerWebExchange exchange, Throwable throwable, long startTime) {
        long duration = (System.nanoTime() - startTime) / 1_000_000;
        String requestPath = exchange.getAttribute(REQUEST_PATH_ATTR);
        
        logger.error("Request failed: {} after {}ms - {}", 
                requestPath, duration, throwable.getMessage());
        
        // Record error metrics
        recordPerformanceMetrics(exchange, duration, true);
    }
    
    private void recordPerformanceMetrics(ServerWebExchange exchange, long duration, boolean isError) {
        if (meterRegistry == null) return;
        
        String path = exchange.getAttribute(REQUEST_PATH_ATTR);
        String method = exchange.getRequest().getMethod().name();
        String status = isError ? "error" : "success";
        
        // Record request count
        meterRegistry.counter("gateway.requests.total",
                "path", sanitizePath(path),
                "method", method,
                "status", status)
                .increment();
        
        // Record response time distribution
        meterRegistry.timer("gateway.response.time",
                "path", sanitizePath(path),
                "method", method,
                "status", status)
                .record(duration, TimeUnit.MILLISECONDS);
    }
    
    private String sanitizePath(String path) {
        if (path == null) return "unknown";
        
        // Replace dynamic path segments with placeholders to avoid metric explosion
        return path.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[a-f0-9-]{36}", "/{uuid}")
                  .replaceAll("/[a-zA-Z0-9]{20,}", "/{token}");
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        return lowerName.contains("authorization") || 
               lowerName.contains("cookie") || 
               lowerName.contains("token") ||
               lowerName.contains("password");
    }

    @Override
    public int getOrder() {
        // Execute early in the filter chain to capture all requests
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}