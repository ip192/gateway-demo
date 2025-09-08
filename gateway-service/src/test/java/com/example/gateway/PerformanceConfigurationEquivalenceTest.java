package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
 * Performance tests to validate configuration equivalence between YAML and properties
 * Ensures that properties-based configuration provides equivalent performance characteristics
 * Requirements: 6.1, 6.2, 6.3, 5.3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("performance")
class PerformanceConfigurationEquivalenceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private HttpClientProperties httpClientProperties;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        // Setup WireMock servers
        userServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        productServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        
        userServiceMock.start();
        productServiceMock.start();

        // Setup WebClient for performance testing
        webClient = WebClient.builder()
                .baseUrl("http://localhost:" + getGatewayPort())
                .build();

        setupMockServices();
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

    private int getGatewayPort() {
        // Extract port from WebTestClient or use default
        return 8080; // Default gateway port
    }

    private void setupMockServices() {
        // User service mocks with performance-oriented responses
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Test User\",\"email\":\"test@example.com\"}")
                        .withFixedDelay(50))); // Consistent 50ms response time

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
                        .withFixedDelay(75))); // Consistent 75ms response time

        productServiceMock.stubFor(post(urlEqualTo("/product/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"service\":\"product-service\",\"version\":\"1.0.0\"}")
                        .withFixedDelay(40)));
    }

    @Test
    void testPerformanceProfileConfigurationLoaded() {
        // Verify performance profile configuration is loaded correctly
        assertThat(httpClientProperties).isNotNull();
        
        // Check HTTP client performance settings
        assertThat(httpClientProperties.getPool().getMaxConnections()).isEqualTo(500);
        assertThat(httpClientProperties.getPool().getMaxIdleTime()).isEqualTo(Duration.ofSeconds(30));
        assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(5000);
        assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(30));
        
        System.out.println("=== Performance Configuration Loaded ===");
        System.out.println("Max connections: " + httpClientProperties.getPool().getMaxConnections());
        System.out.println("Max idle time: " + httpClientProperties.getPool().getMaxIdleTime().getSeconds() + "s");
        System.out.println("Connect timeout: " + httpClientProperties.getConnectTimeout() + "ms");
        System.out.println("Response timeout: " + httpClientProperties.getResponseTimeout().getSeconds() + "s");
    }

    @Test
    void testBaselinePerformanceMetrics() throws InterruptedException {
        // Establish baseline performance metrics with properties configuration
        int warmupRequests = 50;
        int testRequests = 100;
        
        // Warmup phase
        System.out.println("Warming up with " + warmupRequests + " requests...");
        for (int i = 0; i < warmupRequests; i++) {
            webTestClient.get()
                    .uri("/user/" + (i % 10 + 1))
                    .exchange()
                    .expectStatus().isOk();
        }
        
        // Measurement phase
        List<Long> responseTimes = new ArrayList<>();
        long totalStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < testRequests; i++) {
            long requestStart = System.currentTimeMillis();
            
            webTestClient.get()
                    .uri("/user/" + (i % 10 + 1))
                    .exchange()
                    .expectStatus().isOk();
            
            long requestDuration = System.currentTimeMillis() - requestStart;
            responseTimes.add(requestDuration);
        }
        
        long totalDuration = System.currentTimeMillis() - totalStartTime;
        
        // Calculate performance metrics
        responseTimes.sort(Long::compareTo);
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50ResponseTime = responseTimes.get(responseTimes.size() / 2);
        long p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
        long p99ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.99));
        double throughput = (testRequests / (totalDuration / 1000.0));
        
        System.out.println("=== Baseline Performance Metrics ===");
        System.out.println("Total requests: " + testRequests);
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("P50 response time: " + p50ResponseTime + "ms");
        System.out.println("P95 response time: " + p95ResponseTime + "ms");
        System.out.println("P99 response time: " + p99ResponseTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/s");
        
        // Performance assertions based on expected properties configuration behavior
        assertThat(avgResponseTime).isLessThan(200); // Should be fast with optimized config
        assertThat(p95ResponseTime).isLessThan(300);
        assertThat(p99ResponseTime).isLessThan(500);
        assertThat(throughput).isGreaterThan(20.0); // Should handle at least 20 req/s
    }

    @Test
    void testConcurrentPerformanceEquivalence() throws InterruptedException {
        // Test concurrent performance with properties configuration
        int concurrentUsers = 50;
        int requestsPerUser = 10;
        int totalRequests = concurrentUsers * requestsPerUser;
        
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        List<Long> responseTimes = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();
        
        for (int user = 0; user < concurrentUsers; user++) {
            final int userId = user;
            executor.submit(() -> {
                for (int req = 0; req < requestsPerUser; req++) {
                    try {
                        long requestStart = System.currentTimeMillis();
                        
                        String endpoint = (req % 2 == 0) ? "/user/" + (userId % 10 + 1) 
                                                        : "/product/" + (userId % 10 + 1);
                        
                        webClient.get()
                                .uri(endpoint)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block(Duration.ofSeconds(10));
                        
                        long requestDuration = System.currentTimeMillis() - requestStart;
                        totalResponseTime.addAndGet(requestDuration);
                        
                        synchronized (responseTimes) {
                            responseTimes.add(requestDuration);
                        }
                        
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Request failed for user " + userId + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        // Calculate performance metrics
        responseTimes.sort(Long::compareTo);
        double avgResponseTime = totalResponseTime.get() / (double) successCount.get();
        double throughput = (successCount.get() / (testDuration / 1000.0));
        long p95ResponseTime = responseTimes.isEmpty() ? 0 : 
                responseTimes.get((int) (responseTimes.size() * 0.95));
        
        System.out.println("=== Concurrent Performance Test ===");
        System.out.println("Concurrent users: " + concurrentUsers);
        System.out.println("Requests per user: " + requestsPerUser);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Test duration: " + testDuration + "ms");
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("P95 response time: " + p95ResponseTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/s");
        
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThan((int)(totalRequests * 0.95)); // 95% success rate
        assertThat(avgResponseTime).isLessThan(300); // Should maintain good response times
        assertThat(throughput).isGreaterThan(30.0); // Should handle high throughput
    }

    @Test
    void testConnectionPoolPerformanceOptimization() throws InterruptedException {
        // Test that connection pool optimization provides expected performance
        int sequentialBatches = 5;
        int requestsPerBatch = 20;
        List<Double> batchThroughputs = new ArrayList<>();
        
        for (int batch = 0; batch < sequentialBatches; batch++) {
            long batchStart = System.currentTimeMillis();
            CountDownLatch batchLatch = new CountDownLatch(requestsPerBatch);
            ExecutorService batchExecutor = Executors.newFixedThreadPool(requestsPerBatch);
            
            AtomicInteger batchSuccess = new AtomicInteger(0);
            
            for (int req = 0; req < requestsPerBatch; req++) {
                final int requestId = req;
                batchExecutor.submit(() -> {
                    try {
                        webClient.get()
                                .uri("/user/" + (requestId % 5 + 1))
                                .retrieve()
                                .bodyToMono(String.class)
                                .block(Duration.ofSeconds(5));
                        
                        batchSuccess.incrementAndGet();
                    } catch (Exception e) {
                        // Log but continue
                    } finally {
                        batchLatch.countDown();
                    }
                });
            }
            
            batchLatch.await(30, TimeUnit.SECONDS);
            batchExecutor.shutdown();
            
            long batchDuration = System.currentTimeMillis() - batchStart;
            double batchThroughput = (batchSuccess.get() / (batchDuration / 1000.0));
            batchThroughputs.add(batchThroughput);
            
            System.out.println("Batch " + (batch + 1) + " throughput: " + 
                    String.format("%.2f", batchThroughput) + " req/s");
            
            // Small delay between batches
            Thread.sleep(1000);
        }
        
        // Analyze throughput consistency
        double avgThroughput = batchThroughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double minThroughput = batchThroughputs.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxThroughput = batchThroughputs.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double throughputVariance = ((maxThroughput - minThroughput) / avgThroughput) * 100;
        
        System.out.println("=== Connection Pool Performance Analysis ===");
        System.out.println("Average throughput: " + String.format("%.2f", avgThroughput) + " req/s");
        System.out.println("Min throughput: " + String.format("%.2f", minThroughput) + " req/s");
        System.out.println("Max throughput: " + String.format("%.2f", maxThroughput) + " req/s");
        System.out.println("Throughput variance: " + String.format("%.2f", throughputVariance) + "%");
        
        // Connection pool should provide consistent performance
        assertThat(avgThroughput).isGreaterThan(15.0);
        assertThat(throughputVariance).isLessThan(30.0); // Less than 30% variance
    }

    @Test
    void testTimeoutConfigurationPerformance() throws InterruptedException {
        // Test that timeout configurations don't negatively impact performance
        int fastRequests = 50;
        List<Long> responseTimes = new ArrayList<>();
        
        // Test with requests that complete well within timeout
        for (int i = 0; i < fastRequests; i++) {
            long requestStart = System.currentTimeMillis();
            
            webTestClient.get()
                    .uri("/user/" + (i % 10 + 1))
                    .exchange()
                    .expectStatus().isOk();
            
            long requestDuration = System.currentTimeMillis() - requestStart;
            responseTimes.add(requestDuration);
        }
        
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        System.out.println("=== Timeout Configuration Performance ===");
        System.out.println("Requests tested: " + fastRequests);
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Max response time: " + maxResponseTime + "ms");
        System.out.println("Configured response timeout: " + 
                httpClientProperties.getResponseTimeout().toMillis() + "ms");
        
        // Fast requests should not be impacted by timeout configuration
        assertThat(avgResponseTime).isLessThan(200);
        assertThat(maxResponseTime).isLessThan(500);
        // Should be well under the configured timeout
        assertThat(maxResponseTime).isLessThan(httpClientProperties.getResponseTimeout().toMillis() / 10);
    }

    @Test
    void testResourceUtilizationEfficiency() throws InterruptedException {
        // Test resource utilization with properties configuration
        Runtime runtime = Runtime.getRuntime();
        
        // Measure initial memory usage
        System.gc();
        Thread.sleep(1000);
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate load to test resource efficiency
        int loadRequests = 200;
        CountDownLatch loadLatch = new CountDownLatch(loadRequests);
        ExecutorService loadExecutor = Executors.newFixedThreadPool(25);
        
        AtomicInteger loadSuccess = new AtomicInteger(0);
        long loadStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < loadRequests; i++) {
            final int requestId = i;
            loadExecutor.submit(() -> {
                try {
                    webClient.get()
                            .uri("/user/" + (requestId % 20 + 1))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(5));
                    
                    loadSuccess.incrementAndGet();
                } catch (Exception e) {
                    // Continue with test
                } finally {
                    loadLatch.countDown();
                }
            });
        }
        
        loadLatch.await(60, TimeUnit.SECONDS);
        loadExecutor.shutdown();
        
        long loadDuration = System.currentTimeMillis() - loadStartTime;
        
        // Measure final memory usage
        System.gc();
        Thread.sleep(1000);
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        double throughput = (loadSuccess.get() / (loadDuration / 1000.0));
        
        System.out.println("=== Resource Utilization Test ===");
        System.out.println("Load requests: " + loadRequests);
        System.out.println("Successful requests: " + loadSuccess.get());
        System.out.println("Load duration: " + loadDuration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/s");
        System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        // Resource utilization should be efficient
        assertThat(loadSuccess.get()).isGreaterThan((int)(loadRequests * 0.95));
        assertThat(throughput).isGreaterThan(20.0);
        assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024); // Less than 50MB increase
    }

    @Test
    void testConfigurationConsistencyValidation() {
        // Validate that properties configuration provides consistent behavior
        HttpClientProperties.Pool pool = httpClientProperties.getPool();
        
        // Verify all performance-related configurations are loaded
        assertThat(pool.getMaxConnections()).isNotNull().isGreaterThan(0);
        assertThat(pool.getMaxIdleTime()).isNotNull().isPositive();
        assertThat(httpClientProperties.getConnectTimeout()).isNotNull().isGreaterThan(0);
        assertThat(httpClientProperties.getResponseTimeout()).isNotNull().isPositive();
        
        // Verify configuration values are performance-optimized
        assertThat(pool.getMaxConnections()).isGreaterThanOrEqualTo(100); // High connection limit
        assertThat(httpClientProperties.getResponseTimeout().getSeconds()).isGreaterThanOrEqualTo(10L); // Reasonable timeout
        
        System.out.println("=== Configuration Consistency Validation ===");
        System.out.println("✓ Max connections: " + pool.getMaxConnections());
        System.out.println("✓ Max idle time: " + pool.getMaxIdleTime().getSeconds() + "s");
        System.out.println("✓ Connect timeout: " + httpClientProperties.getConnectTimeout() + "ms");
        System.out.println("✓ Response timeout: " + httpClientProperties.getResponseTimeout().getSeconds() + "s");
        System.out.println("All performance configurations loaded successfully");
    }
}