package com.example.gateway.controller;

import com.example.gateway.config.OptimizedRouteLocator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controller for exposing performance metrics and statistics.
 * Provides endpoints for monitoring gateway performance.
 */
@RestController
@RequestMapping("/actuator/performance")
public class PerformanceController {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    @Autowired(required = false)
    private OptimizedRouteLocator optimizedRouteLocator;

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory metrics
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        Map<String, Object> memoryMetrics = new HashMap<>();
        memoryMetrics.put("heap_used_mb", heapUsage.getUsed() / 1024 / 1024);
        memoryMetrics.put("heap_max_mb", heapUsage.getMax() / 1024 / 1024);
        memoryMetrics.put("heap_usage_percent", 
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        memoryMetrics.put("non_heap_used_mb", nonHeapUsage.getUsed() / 1024 / 1024);
        
        metrics.put("memory", memoryMetrics);
        
        // Gateway-specific metrics
        if (meterRegistry != null) {
            Map<String, Object> gatewayMetrics = new HashMap<>();
            
            // Request metrics
            Timer requestTimer = meterRegistry.find("gateway.request.processing.time").timer();
            if (requestTimer != null) {
                gatewayMetrics.put("total_requests", requestTimer.count());
                gatewayMetrics.put("avg_response_time_ms", requestTimer.mean(TimeUnit.MILLISECONDS));
                gatewayMetrics.put("max_response_time_ms", requestTimer.max(TimeUnit.MILLISECONDS));
            }
            
            // Route cache metrics
            if (optimizedRouteLocator != null) {
                OptimizedRouteLocator.CacheStats cacheStats = optimizedRouteLocator.getCacheStats();
                gatewayMetrics.put("cached_routes", cacheStats.getCachedRoutes());
                gatewayMetrics.put("config_hash", cacheStats.getConfigHash());
            }
            
            metrics.put("gateway", gatewayMetrics);
        }
        
        // JVM metrics
        Map<String, Object> jvmMetrics = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        jvmMetrics.put("available_processors", runtime.availableProcessors());
        jvmMetrics.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
        jvmMetrics.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
        jvmMetrics.put("used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        
        metrics.put("jvm", jvmMetrics);
        
        return ResponseEntity.ok(metrics);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getPerformanceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // Memory health check
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        
        String memoryStatus = heapUsagePercent > 90 ? "CRITICAL" : 
                             heapUsagePercent > 75 ? "WARNING" : "HEALTHY";
        
        health.put("memory_status", memoryStatus);
        health.put("memory_usage_percent", heapUsagePercent);
        
        // Response time health check
        if (meterRegistry != null) {
            Timer requestTimer = meterRegistry.find("gateway.request.processing.time").timer();
            if (requestTimer != null) {
                double avgResponseTime = requestTimer.mean(TimeUnit.MILLISECONDS);
                String responseTimeStatus = avgResponseTime > 5000 ? "CRITICAL" :
                                          avgResponseTime > 1000 ? "WARNING" : "HEALTHY";
                
                health.put("response_time_status", responseTimeStatus);
                health.put("avg_response_time_ms", avgResponseTime);
            }
        }
        
        // Overall health
        boolean isHealthy = "HEALTHY".equals(health.get("memory_status")) && 
                           "HEALTHY".equals(health.get("response_time_status"));
        
        health.put("overall_status", isHealthy ? "HEALTHY" : "DEGRADED");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearRouteCache() {
        Map<String, String> response = new HashMap<>();
        
        if (optimizedRouteLocator != null) {
            optimizedRouteLocator.clearCache();
            response.put("status", "success");
            response.put("message", "Route cache cleared successfully");
        } else {
            response.put("status", "error");
            response.put("message", "Optimized route locator not available");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> triggerGarbageCollection() {
        Map<String, Object> response = new HashMap<>();
        
        // Record memory before GC
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        // Trigger GC
        System.gc();
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Record memory after GC
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryFreed = memoryBefore - memoryAfter;
        
        response.put("memory_before_mb", memoryBefore / 1024 / 1024);
        response.put("memory_after_mb", memoryAfter / 1024 / 1024);
        response.put("memory_freed_mb", memoryFreed / 1024 / 1024);
        response.put("gc_triggered", true);
        
        return ResponseEntity.ok(response);
    }
}