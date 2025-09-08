package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance benchmark tests to compare different routing approaches
 * and measure optimization improvements.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PerformanceBenchmarkTest {

    @LocalServerPort
    private int gatewayPort;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        userServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        productServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        
        userServiceMock.start();
        productServiceMock.start();

        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + gatewayPort)
                .build();

        setupOptimizedMockResponses();
    }

    @AfterEach
    void tearDown() {
        if (userServiceMock != null && userServiceMock.isRunning()) {
            userServiceMock.stop();
        }
        if (productServiceMock != null && productServiceMock.isRunning()) {
            productServiceMock.stop();
        }
    }

    private void setupOptimizedMockResponses() {
        // Fast responses for performance testing
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"User\"}")
                        .withFixedDelay(10))); // Minimal delay

        productServiceMock.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Product\"}")
                        .withFixedDelay(10)));
    }

    @Test
    void benchmarkRouteMatchingPerformance() {
        System.out.println("=== Route Matching Performance Benchmark ===");
        
        // Test different route patterns
        String[] routes = {
                "/user/1",           // Simple path
                "/user/profile/123", // Nested path
                "/product/search",   // Different service
                "/api/v1/users/1"    // API versioned path
        };
        
        for (String route : routes) {
            long totalTime = 0;
            int iterations = 1000;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                
                try {
                    webClient.get()
                            .uri(route)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(2));
                } catch (Exception e) {
                    // Some routes may not exist, that's ok for timing
                }
                
                totalTime += (System.nanoTime() - startTime);
            }
            
            double averageTime = (totalTime / iterations) / 1_000_000.0; // Convert to ms
            System.out.println(String.format("Route %s: %.3f ms average", route, averageTime));
        }
    }

    @Test
    void benchmarkThroughputOptimization() throws InterruptedException {
        System.out.println("=== Throughput Optimization Benchmark ===");
        
        int[] concurrencyLevels = {1, 10, 50, 100, 200};
        
        for (int concurrency : concurrencyLevels) {
            BenchmarkResult result = runThroughputTest(concurrency, 1000);
            
            System.out.println(String.format(
                    "Concurrency %d: %.2f req/s, %.2f ms avg, %.2f%% success",
                    concurrency,
                    result.throughput,
                    result.averageResponseTime,
                    result.successRate * 100
            ));
        }
    }

    @Test
    void benchmarkMemoryEfficiency() throws InterruptedException {
        System.out.println("=== Memory Efficiency Benchmark ===");
        
        Runtime runtime = Runtime.getRuntime();
        
        // Baseline memory
        System.gc();
        Thread.sleep(1000);
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Run load test
        runThroughputTest(100, 5000);
        
        // Measure memory after load
        System.gc();
        Thread.sleep(1000);
        long afterLoadMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryIncrease = afterLoadMemory - baselineMemory;
        double memoryPerRequest = memoryIncrease / 5000.0;
        
        System.out.println(String.format("Baseline memory: %.2f MB", baselineMemory / 1024.0 / 1024.0));
        System.out.println(String.format("After load memory: %.2f MB", afterLoadMemory / 1024.0 / 1024.0));
        System.out.println(String.format("Memory increase: %.2f MB", memoryIncrease / 1024.0 / 1024.0));
        System.out.println(String.format("Memory per request: %.2f KB", memoryPerRequest / 1024.0));
        
        // Memory efficiency should be good
        assertThat(memoryPerRequest).isLessThan(10 * 1024); // Less than 10KB per request
    }

    @Test
    void benchmarkFilterChainOptimization() throws InterruptedException {
        System.out.println("=== Filter Chain Optimization Benchmark ===");
        
        // Test with different request types to measure filter impact
        String[] endpoints = {
                "/user/1",      // Standard request
                "/product/1",   // Different service
                "/health"       // Health check (if available)
        };
        
        for (String endpoint : endpoints) {
            long totalFilterTime = 0;
            int iterations = 500;
            
            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                
                try {
                    webClient.get()
                            .uri(endpoint)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(2));
                } catch (Exception e) {
                    // Ignore errors for timing test
                }
                
                totalFilterTime += (System.nanoTime() - startTime);
            }
            
            double averageFilterTime = (totalFilterTime / iterations) / 1_000_000.0;
            System.out.println(String.format("Endpoint %s: %.3f ms average (including filters)", 
                    endpoint, averageFilterTime));
        }
    }

    @Test
    void benchmarkReactiveStreamPerformance() {
        System.out.println("=== Reactive Stream Performance Benchmark ===");
        
        // Test reactive stream processing efficiency
        long startTime = System.currentTimeMillis();
        
        Flux<String> responses = Flux.range(1, 100)
                .flatMap(i -> 
                    webClient.get()
                            .uri("/user/" + i)
                            .retrieve()
                            .bodyToMono(String.class)
                            .onErrorReturn("error")
                )
; // Process in batches
        
        List<String> results = responses.collectList().block(Duration.ofSeconds(30));
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println(String.format("Processed 100 requests in %d ms", duration));
        System.out.println(String.format("Reactive throughput: %.2f req/s", 100.0 / (duration / 1000.0)));
        
        assertThat(results).isNotNull();
        assertThat(duration).isLessThan(10000); // Should complete within 10 seconds
    }

    private BenchmarkResult runThroughputTest(int concurrency, int totalRequests) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    webClient.get()
                            .uri("/user/" + (requestId % 100 + 1))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(5));
                    
                    long requestDuration = System.currentTimeMillis() - requestStart;
                    totalResponseTime.addAndGet(requestDuration);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    // Count as failure
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        BenchmarkResult result = new BenchmarkResult();
        result.throughput = (successCount.get() / (testDuration / 1000.0));
        result.averageResponseTime = totalResponseTime.get() / (double) Math.max(successCount.get(), 1);
        result.successRate = successCount.get() / (double) totalRequests;
        
        return result;
    }

    private static class BenchmarkResult {
        double throughput;
        double averageResponseTime;
        double successRate;
    }
}