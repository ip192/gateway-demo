package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for dynamic routing functionality.
 * Binds to gateway.dynamic-routing.* properties in application.properties.
 */
@Component
@ConfigurationProperties(prefix = "gateway.dynamic-routing")
public class DynamicRoutingProperties {
    
    private boolean enabled = true;
    private Timeout timeout = new Timeout();
    private Retry retry = new Retry();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Timeout getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }
    
    public Retry getRetry() {
        return retry;
    }
    
    public void setRetry(Retry retry) {
        this.retry = retry;
    }
    
    /**
     * Timeout configuration for HTTP requests.
     */
    public static class Timeout {
        private int connect = 5000; // 5 seconds default
        private int read = 10000;   // 10 seconds default
        
        public int getConnect() {
            return connect;
        }
        
        public void setConnect(int connect) {
            this.connect = connect;
        }
        
        public int getRead() {
            return read;
        }
        
        public void setRead(int read) {
            this.read = read;
        }
    }
    
    /**
     * Retry configuration for failed requests.
     */
    public static class Retry {
        private int maxAttempts = 3;    // 3 attempts default
        private long delay = 1000;      // 1 second delay default
        
        public int getMaxAttempts() {
            return maxAttempts;
        }
        
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
        
        public long getDelay() {
            return delay;
        }
        
        public void setDelay(long delay) {
            this.delay = delay;
        }
    }
}