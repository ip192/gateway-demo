package com.example.gateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuration for performance monitoring and metrics collection.
 * Provides custom metrics for gateway performance analysis.
 */
@Configuration
public class PerformanceMonitoringConfiguration {

    @Bean
    public GlobalFilter performanceMetricsFilter(MeterRegistry meterRegistry) {
        return new PerformanceMetricsFilter(meterRegistry);
    }

    /**
     * Custom filter to collect detailed performance metrics
     */
    public static class PerformanceMetricsFilter implements GlobalFilter, Ordered {
        
        private final MeterRegistry meterRegistry;
        private final Timer routeMatchingTimer;
        private final Timer filterExecutionTimer;
        private final Timer totalRequestTimer;

        public PerformanceMetricsFilter(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            this.routeMatchingTimer = Timer.builder("gateway.route.matching.time")
                    .description("Time taken for route matching")
                    .register(meterRegistry);
            this.filterExecutionTimer = Timer.builder("gateway.filter.execution.time")
                    .description("Time taken for filter execution")
                    .register(meterRegistry);
            this.totalRequestTimer = Timer.builder("gateway.request.total.time")
                    .description("Total request processing time")
                    .register(meterRegistry);
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            long startTime = System.nanoTime();
            
            return chain.filter(exchange)
                    .doOnSuccess(aVoid -> recordMetrics(exchange, startTime, null))
                    .doOnError(throwable -> recordMetrics(exchange, startTime, throwable));
        }

        private void recordMetrics(ServerWebExchange exchange, long startTime, Throwable error) {
            long duration = System.nanoTime() - startTime;
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            String status = error != null ? "error" : "success";

            // Record custom metrics
            meterRegistry.counter("gateway.requests.total", 
                    "path", path, "method", method, "status", status)
                    .increment();

            meterRegistry.timer("gateway.request.duration", 
                    "path", path, "method", method, "status", status)
                    .record(duration, java.util.concurrent.TimeUnit.NANOSECONDS);

            // Record memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            meterRegistry.gauge("gateway.memory.used", usedMemory);
            meterRegistry.gauge("gateway.memory.total", runtime.totalMemory());
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}