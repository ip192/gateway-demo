package com.example.gateway.model;

/**
 * Metadata configuration for routes including timeout, enabled status, and priority
 */
public class RouteMetadata {
    private int timeout = 5000; // Default 5 seconds
    private boolean enabled = true; // Default enabled
    private int order = 0; // Default order

    public RouteMetadata() {}

    public RouteMetadata(int timeout, boolean enabled, int order) {
        this.timeout = timeout;
        this.enabled = enabled;
        this.order = order;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "RouteMetadata{" +
                "timeout=" + timeout +
                ", enabled=" + enabled +
                ", order=" + order +
                '}';
    }
}