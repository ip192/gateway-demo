package com.example.gateway.model;

/**
 * Configuration model for circuit breaker settings
 */
public class CircuitBreakerConfig {
    private int failureRateThreshold = 50; // Percentage
    private int waitDurationInOpenState = 10000; // Milliseconds
    private int slidingWindowSize = 10; // Number of calls
    private int minimumNumberOfCalls = 5; // Minimum calls before circuit breaker can trip

    public CircuitBreakerConfig() {}

    public CircuitBreakerConfig(int failureRateThreshold, int waitDurationInOpenState, 
                               int slidingWindowSize, int minimumNumberOfCalls) {
        this.failureRateThreshold = failureRateThreshold;
        this.waitDurationInOpenState = waitDurationInOpenState;
        this.slidingWindowSize = slidingWindowSize;
        this.minimumNumberOfCalls = minimumNumberOfCalls;
    }

    public int getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(int failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public int getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public void setWaitDurationInOpenState(int waitDurationInOpenState) {
        this.waitDurationInOpenState = waitDurationInOpenState;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
    }

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
        this.minimumNumberOfCalls = minimumNumberOfCalls;
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfig{" +
                "failureRateThreshold=" + failureRateThreshold +
                ", waitDurationInOpenState=" + waitDurationInOpenState +
                ", slidingWindowSize=" + slidingWindowSize +
                ", minimumNumberOfCalls=" + minimumNumberOfCalls +
                '}';
    }
}