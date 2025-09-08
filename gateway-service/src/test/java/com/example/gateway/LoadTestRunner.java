package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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

/**
 * Standalone load test runner for performance testing.
 * Can be used to generate load against the gateway for performance analysis.
 */
public class LoadTestRunner {
    
    private static final String GATEWAY_BASE_URL = "http://localhost:8080";
    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;
    private WebClient webClient;
    
    public static void main(String[] args) {
        LoadTestRunner runner = new LoadTestRunner();
        try {
            runner.setup();
            runner.runLoadTests();
        } finally {
            runner.tearDown();
        }
    }
    
    public void setup() {
        // Setup mock services
        userServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        productServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        
        userServiceMock.start();
        productServiceMock.start();
        
        // Setup web client
        webClient = WebClient.builder()
                .baseUrl(GATEWAY_BASE_URL)
                .build();
        
        setupMockResponses();
        
        System.out.println("Load test setup completed");
    }
    
    public void tearDown() {
        if (userServiceMock != null && userServiceMock.isRunning()) {
            userServiceMock.stop();
        }
        if (productServiceMock != null && productServiceMock.isRunning()) {
            productServiceMock.stop();
        }
        System.out.println("Load test teardown completed");
    }
    
    private void setupMockResponses() {
        // Fast user service responses
        userServiceMock.stubFor(get(urlPathMatching("/user/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Test User\"}")
                        .withFixedDelay(20)));
        
        // Fast product service responses
        productServiceMock.stubFor(get(urlPathMatching("/product/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Test Product\"}")
                        .withFixedDelay(25)));
    }
    
    public void runLoadTests() {
        System.out.println("=== Starting Load Tests ===");
        
        // Test 1: Baseline performance
        runBaselineTest();
        
        // Test 2: Concurrent load
        runConcurrentLoadTest();
        
        // Test 3: Sustained load
        runSustainedLoadTest();
        
        // Test 4: Spike test
        runSpikeTest();
        
        System.out.println("=== Load Tests Completed ===");
    }
    
    private void runBaselineTest() {
        System.out.println("\n--- Baseline Performance Test ---");
        
        int iterations = 100;
        long totalTime = 0;
        int successCount = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            
            try {
                webClient.get()
                        .uri("/user/" + (i % 10 + 1))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(5));
                
                successCount++;
            } catch (Exception e) {
                System.err.println("Request failed: " + e.getMessage());
            }
            
            totalTime += (System.currentTimeMillis() - startTime);
        }
        
        double avgTime = totalTime / (double) iterations;
        double successRate = (successCount / (double) iterations) * 100;
        
        System.out.println("Baseline Results:");
        System.out.println("  Requests: " + iterations);
        System.out.println("  Success Rate: " + String.format("%.1f%%", successRate));
        System.out.println("  Average Response Time: " + String.format("%.2f ms", avgTime));
    }
    
    private void runConcurrentLoadTest() {
        System.out.println("\n--- Concurrent Load Test ---");
        
        int concurrency = 100;
        int requestsPerThread = 50;
        int totalRequests = concurrency * requestsPerThread;
        
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        
        for (int thread = 0; thread < concurrency; thread++) {
            final int threadId = thread;
            executor.submit(() -> {
                for (int req = 0; req < requestsPerThread; req++) {
                    try {
                        long requestStart = System.currentTimeMillis();
                        
                        String endpoint = (req % 2 == 0) ? "/user/" + (req % 10 + 1) 
                                                         : "/product/" + (req % 10 + 1);
                        
                        webClient.get()
                                .uri(endpoint)
                                .retrieve()
                                .bodyToMono(String.class)
                                .block(Duration.ofSeconds(10));
                        
                        long requestDuration = System.currentTimeMillis() - requestStart;
                        totalResponseTime.addAndGet(requestDuration);
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        // Count as failure
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        try {
            boolean completed = latch.await(120, TimeUnit.SECONDS);
            executor.shutdown();
            
            long testDuration = System.currentTimeMillis() - testStartTime;
            double avgResponseTime = totalResponseTime.get() / (double) Math.max(successCount.get(), 1);
            double throughput = (successCount.get() / (testDuration / 1000.0));
            double successRate = (successCount.get() / (double) totalRequests) * 100;
            
            System.out.println("Concurrent Load Results:");
            System.out.println("  Total Requests: " + totalRequests);
            System.out.println("  Concurrency: " + concurrency);
            System.out.println("  Success Rate: " + String.format("%.1f%%", successRate));
            System.out.println("  Average Response Time: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("  Throughput: " + String.format("%.2f req/s", throughput));
            System.out.println("  Test Duration: " + testDuration + " ms");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Concurrent load test interrupted");
        }
    }
    
    private void runSustainedLoadTest() {
        System.out.println("\n--- Sustained Load Test ---");
        
        int requestsPerSecond = 100;
        int durationSeconds = 30;
        int totalRequests = requestsPerSecond * durationSeconds;
        
        CountDownLatch latch = new CountDownLatch(totalRequests);
        ExecutorService executor = Executors.newScheduledThreadPool(20);
        
        AtomicInteger successCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();
        
        long testStartTime = System.currentTimeMillis();
        
        // Schedule requests at regular intervals
        for (int second = 0; second < durationSeconds; second++) {
            for (int req = 0; req < requestsPerSecond; req++) {
                final int requestId = second * requestsPerSecond + req;
                
                ((java.util.concurrent.ScheduledExecutorService) executor).schedule(() -> {
                    try {
                        long requestStart = System.currentTimeMillis();
                        
                        String endpoint = (requestId % 3 == 0) ? "/user/" + (requestId % 20 + 1) :
                                        (requestId % 3 == 1) ? "/product/" + (requestId % 20 + 1) :
                                                              "/user/info";
                        
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
                        // Count as failure
                    } finally {
                        latch.countDown();
                    }
                }, second * 1000L, TimeUnit.MILLISECONDS);
            }
        }
        
        try {
            boolean completed = latch.await(durationSeconds + 30, TimeUnit.SECONDS);
            executor.shutdown();
            
            long testDuration = System.currentTimeMillis() - testStartTime;
            
            // Calculate statistics
            responseTimes.sort(Long::compareTo);
            double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95ResponseTime = responseTimes.isEmpty() ? 0 : 
                    responseTimes.get((int) (responseTimes.size() * 0.95));
            long p99ResponseTime = responseTimes.isEmpty() ? 0 : 
                    responseTimes.get((int) (responseTimes.size() * 0.99));
            double actualThroughput = (successCount.get() / (testDuration / 1000.0));
            double successRate = (successCount.get() / (double) totalRequests) * 100;
            
            System.out.println("Sustained Load Results:");
            System.out.println("  Target: " + requestsPerSecond + " req/s for " + durationSeconds + " seconds");
            System.out.println("  Total Requests: " + totalRequests);
            System.out.println("  Success Rate: " + String.format("%.1f%%", successRate));
            System.out.println("  Average Response Time: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("  95th Percentile: " + p95ResponseTime + " ms");
            System.out.println("  99th Percentile: " + p99ResponseTime + " ms");
            System.out.println("  Actual Throughput: " + String.format("%.2f req/s", actualThroughput));
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Sustained load test interrupted");
        }
    }
    
    private void runSpikeTest() {
        System.out.println("\n--- Spike Test ---");
        
        // Sudden spike to high load
        int spikeRequests = 500;
        CountDownLatch latch = new CountDownLatch(spikeRequests);
        ExecutorService executor = Executors.newFixedThreadPool(100);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        long testStartTime = System.currentTimeMillis();
        
        // Submit all requests at once to create a spike
        for (int i = 0; i < spikeRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    
                    webClient.get()
                            .uri("/user/" + (requestId % 50 + 1))
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(Duration.ofSeconds(15));
                    
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
        
        try {
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();
            
            long testDuration = System.currentTimeMillis() - testStartTime;
            double avgResponseTime = totalResponseTime.get() / (double) Math.max(successCount.get(), 1);
            double throughput = (successCount.get() / (testDuration / 1000.0));
            double successRate = (successCount.get() / (double) spikeRequests) * 100;
            
            System.out.println("Spike Test Results:");
            System.out.println("  Spike Requests: " + spikeRequests);
            System.out.println("  Success Rate: " + String.format("%.1f%%", successRate));
            System.out.println("  Average Response Time: " + String.format("%.2f ms", avgResponseTime));
            System.out.println("  Peak Throughput: " + String.format("%.2f req/s", throughput));
            System.out.println("  Recovery Time: " + testDuration + " ms");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Spike test interrupted");
        }
    }
}