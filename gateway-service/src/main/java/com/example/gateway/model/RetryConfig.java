package com.example.gateway.model;

/**
 * Configuration model for retry settings
 */
public class RetryConfig {
    private int retries = 3; // Default number of retries
    private long firstBackoff = 50; // First backoff in milliseconds
    private long maxBackoff = 500; // Maximum backoff in milliseconds
    private double factor = 2.0; // Backoff multiplier factor

    public RetryConfig() {}

    public RetryConfig(int retries, long firstBackoff, long maxBackoff, double factor) {
        this.retries = retries;
        this.firstBackoff = firstBackoff;
        this.maxBackoff = maxBackoff;
        this.factor = factor;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public long getFirstBackoff() {
        return firstBackoff;
    }

    public void setFirstBackoff(long firstBackoff) {
        this.firstBackoff = firstBackoff;
    }

    public long getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(long maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public double getFactor() {
        return factor;
    }

    public void setFactor(double factor) {
        this.factor = factor;
    }

    @Override
    public String toString() {
        return "RetryConfig{" +
                "retries=" + retries +
                ", firstBackoff=" + firstBackoff +
                ", maxBackoff=" + maxBackoff +
                ", factor=" + factor +
                '}';
    }
}