package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive performance tests for the gateway service.
 * Tests load handling, response times, and resource utilization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PerformanceTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private MeterRegistry meterRegistry;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;
    private WebTestClient webTestClient;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        // Setup WireMock servers
        userServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        productServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        
        userServiceMock.start();
        productServiceMock.start();

        // Setup test clients
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + gatewayPort)
                .responseTimeout(Duration.ofSeconds(30))
                .build();

        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + gatewayPort)
                .build();

        // Setup mock responses
        setupMockResponses();
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

    private void setupMockResponses() {
        // User service mocks
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Test User\",\"email\":\"test@example.com\"}")
                        .withFixedDelay(50))); // 50ms response time

        userServiceMock.stubFor(post(urlEqualTo("/user/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"service\":\"user-service\",\"version\":\"1.0.0\"}")
                        .withFixedDelay(30)));

        // Product service mocks
        productServiceMock.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Test Product\",\"price\":99.99}")
                        .withFixedDelay(75))); // 75ms response time

        productServiceMock.stubFor(post(urlEqualTo("/product/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"service\":\"product-service\",\"version\":\"1.0.0\"}")
                        .withFixedDelay(40)));
    }

    @Test
    void testSingleRequestPerformance() {
        // Measure baseline performance for single requests
        long startTime = System.currentTimeMillis();
        
        webTestClient.get()
                .uri("/user/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Should complete within reasonable time (including mock delay + processing)
        assertThat(duration).isLessThan(200); // 50ms mock + 150ms buffer
        
        System.out.println("Single request duration: " + duration + "ms");
    }

    @Test
    void testConcurrentRequestsPerformance() throws InterruptedException {
        int concurrentRequests = 100;
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    webClient.get()
                            .uri("/user/" + (requestId % 10 + 1))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(10));
                    
                    long requestDuration = System.currentTimeMillis() - requestStart;
                    totalResponseTime.addAndGet(requestDuration);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request " + requestId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        double averageResponseTime = totalResponseTime.get() / (double) successCount.get();
        double throughput = (successCount.get() / (testDuration / 1000.0));
        
        System.out.println("=== Concurrent Requests Performance Test ===");
        System.out.println("Total requests: " + concurrentRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Test duration: " + testDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", averageResponseTime) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
        
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan((int)(concurrentRequests * 0.95)); // 95% success rate
        assertThat(averageResponseTime).isLessThan(500); // Average under 500ms
        assertThat(throughput).isGreaterThan(10.0); // At least 10 requests/second
    }

    @Test
    void testSustainedLoadPerformance() throws InterruptedException {
        int requestsPerSecond = 50;
        int durationSeconds = 10;
        int totalRequests = requestsPerSecond * durationSeconds;
        
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();
        
        // Schedule requests at regular intervals
        for (int second = 0; second < durationSeconds; second++) {
            for (int req = 0; req < requestsPerSecond; req++) {
                final int requestId = second * requestsPerSecond + req;
                
                executor.schedule(() -> {
                    try {
                        long requestStart = System.currentTimeMillis();
                        
                        String endpoint = (requestId % 2 == 0) ? "/user/" + (requestId % 10 + 1) 
                                                              : "/product/" + (requestId % 10 + 1);
                        
                        webClient.get()
                                .uri(endpoint)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block(Duration.ofSeconds(5));
                        
                        long requestDuration = System.currentTimeMillis() - requestStart;
                        synchronized (responseTimes) {
                            responseTimes.add(requestDuration);
                        }
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }, second * 1000L, TimeUnit.MILLISECONDS);
            }
        }
        
        boolean completed = latch.await(durationSeconds + 10, TimeUnit.SECONDS);
        executor.shutdown();
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        // Calculate statistics
        responseTimes.sort(Long::compareTo);
        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.99));
        double actualThroughput = (successCount.get() / (testDuration / 1000.0));
        
        System.out.println("=== Sustained Load Performance Test ===");
        System.out.println("Target: " + requestsPerSecond + " req/s for " + durationSeconds + " seconds");
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Test duration: " + testDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", averageResponseTime) + "ms");
        System.out.println("95th percentile response time: " + p95ResponseTime + "ms");
        System.out.println("99th percentile response time: " + p99ResponseTime + "ms");
        System.out.println("Actual throughput: " + String.format("%.2f", actualThroughput) + " requests/second");
        
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan((int)(totalRequests * 0.90)); // 90% success rate
        assertThat(averageResponseTime).isLessThan(300);
        assertThat(p95ResponseTime).isLessThan(500);
        assertThat(actualThroughput).isGreaterThan(requestsPerSecond * 0.8); // 80% of target throughput
    }

    @Test
    void testMemoryUsageUnderLoad() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        
        // Record initial memory usage
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        int requestCount = 1000;
        CountDownLatch latch = new CountDownLatch(requestCount);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        
        for (int i = 0; i < requestCount; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    webClient.get()
                            .uri("/user/" + (requestId % 100 + 1))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(5));
                } catch (Exception e) {
                    // Ignore errors for memory test
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("=== Memory Usage Test ===");
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // Memory increase should be reasonable (less than 100MB for 1000 requests)
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
    }

    @Test
    void testRouteMatchingPerformance() {
        // Test performance of route matching with different path patterns
        String[] testPaths = {
                "/user/1",
                "/user/profile/123",
                "/product/search?q=test",
                "/product/category/electronics",
                "/api/v1/users/1/orders",
                "/health",
                "/actuator/metrics"
        };
        
        for (String path : testPaths) {
            long startTime = System.nanoTime();
            
            try {
                webTestClient.get()
                        .uri(path)
                        .exchange();
            } catch (Exception e) {
                // Expected for some paths that don't have routes
            }
            
            long duration = System.nanoTime() - startTime;
            System.out.println("Route matching for " + path + ": " + (duration / 1_000_000) + "ms");
            
            // Route matching should be very fast (under 10ms)
            assertThat(duration / 1_000_000).isLessThan(10);
        }
    }

    @Test
    void testFilterExecutionPerformance() {
        // Test the performance impact of filters
        long startTime = System.currentTimeMillis();
        
        // Make multiple requests to measure filter overhead
        for (int i = 0; i < 100; i++) {
            webTestClient.get()
                    .uri("/user/" + (i % 10 + 1))
                    .exchange()
                    .expectStatus().isOk();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        double averagePerRequest = duration / 100.0;
        
        System.out.println("=== Filter Performance Test ===");
        System.out.println("100 requests completed in: " + duration + "ms");
        System.out.println("Average time per request: " + String.format("%.2f", averagePerRequest) + "ms");
        
        // Filter overhead should be minimal
        assertThat(averagePerRequest).isLessThan(200); // Including mock service delay
    }

    @Test
    void testMetricsCollection() {
        // Make some requests to generate metrics
        for (int i = 0; i < 10; i++) {
            webTestClient.get()
                    .uri("/user/" + (i + 1))
                    .exchange()
                    .expectStatus().isOk();
        }
        
        // Verify metrics are being collected
        Timer requestTimer = meterRegistry.find("gateway.request.total.time").timer();
        if (requestTimer != null) {
            assertThat(requestTimer.count()).isGreaterThan(0);
            System.out.println("Total requests recorded: " + requestTimer.count());
            System.out.println("Average request time: " + 
                    String.format("%.2f", requestTimer.mean(TimeUnit.MILLISECONDS)) + "ms");
        }
        
        // Check custom metrics
        assertThat(meterRegistry.find("gateway.requests.total").counter()).isNotNull();
        assertThat(meterRegistry.find("gateway.request.duration").timer()).isNotNull();
    }
}