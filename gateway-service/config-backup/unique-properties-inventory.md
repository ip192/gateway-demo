# Unique Configuration Properties Inventory

## Server Configuration Properties
- `server.port`
- `server.netty.connection-timeout`
- `server.netty.h2c-max-content-length`
- `server.compression.enabled`
- `server.compression.mime-types`

## Spring Application Properties
- `spring.application.name`

## Spring Cloud Gateway Properties

### Route Configuration
- `spring.cloud.gateway.routes[].id`
- `spring.cloud.gateway.routes[].uri`
- `spring.cloud.gateway.routes[].predicates[]`
- `spring.cloud.gateway.routes[].filters[].name`
- `spring.cloud.gateway.routes[].filters[].args.*`
- `spring.cloud.gateway.routes[].metadata.*`

### HTTP Client Configuration
- `spring.cloud.gateway.httpclient.connect-timeout`
- `spring.cloud.gateway.httpclient.response-timeout`
- `spring.cloud.gateway.httpclient.pool.max-connections`
- `spring.cloud.gateway.httpclient.pool.max-idle-time`

### Metrics Configuration
- `spring.cloud.gateway.metrics.enabled`

## Resilience4j Properties

### Circuit Breaker Configuration
- `resilience4j.circuitbreaker.instances.{name}.slidingWindowSize`
- `resilience4j.circuitbreaker.instances.{name}.minimumNumberOfCalls`
- `resilience4j.circuitbreaker.instances.{name}.failureRateThreshold`
- `resilience4j.circuitbreaker.instances.{name}.waitDurationInOpenState`
- `resilience4j.circuitbreaker.instances.{name}.permittedNumberOfCallsInHalfOpenState`
- `resilience4j.circuitbreaker.instances.{name}.registerHealthIndicator`
- `resilience4j.circuitbreaker.instances.{name}.slowCallRateThreshold`
- `resilience4j.circuitbreaker.instances.{name}.slowCallDurationThreshold`

### Time Limiter Configuration
- `resilience4j.timelimiter.instances.{name}.timeoutDuration`

### Retry Configuration
- `resilience4j.retry.instances.{name}.maxAttempts`
- `resilience4j.retry.instances.{name}.waitDuration`
- `resilience4j.retry.instances.{name}.exponentialBackoffMultiplier`

## Management Properties

### Endpoints Configuration
- `management.endpoints.web.exposure.include`
- `management.endpoint.health.show-details`
- `management.endpoint.refresh.enabled`
- `management.endpoint.metrics.enabled`

### Health Configuration
- `management.health.circuitbreakers.enabled`

### Metrics Configuration
- `management.metrics.export.prometheus.enabled`
- `management.metrics.distribution.percentiles-histogram.*`
- `management.metrics.distribution.percentiles.*`
- `management.metrics.tags.application`
- `management.metrics.tags.environment`

## Logging Properties
- `logging.level.*`
- `logging.pattern.console`

## Custom Gateway Properties (Dynamic Routing)

### Route Configuration
- `gateway.routes[].id`
- `gateway.routes[].uri`
- `gateway.routes[].predicates[].name`
- `gateway.routes[].predicates[].args.*`
- `gateway.routes[].filters[].name`
- `gateway.routes[].filters[].args.*`
- `gateway.routes[].metadata.*`

### Global Circuit Breaker Configuration
- `gateway.circuitBreaker.failureRateThreshold`
- `gateway.circuitBreaker.waitDurationInOpenState`
- `gateway.circuitBreaker.slidingWindowSize`
- `gateway.circuitBreaker.minimumNumberOfCalls`

### Global Retry Configuration
- `gateway.retry.retries`
- `gateway.retry.firstBackoff`
- `gateway.retry.maxBackoff`
- `gateway.retry.factor`

## Route Filter Arguments

### CircuitBreaker Filter
- `name` - Circuit breaker instance name
- `fallbackUri` - Fallback URI for circuit breaker

### Retry Filter
- `retries` - Number of retry attempts
- `statuses` - HTTP statuses to retry on
- `methods` - HTTP methods to retry
- `backoff.firstBackoff` - Initial backoff duration
- `backoff.maxBackoff` - Maximum backoff duration
- `backoff.factor` - Backoff multiplier factor
- `backoff.basedOnPreviousValue` - Whether to base backoff on previous value

### AddRequestHeader Filter
- `name` - Header name
- `value` - Header value

### AddResponseHeader Filter
- `name` - Header name
- `value` - Header value

### StripPrefix Filter
- `parts` - Number of path parts to strip

## Route Predicate Arguments

### Path Predicate
- `pattern` - Path pattern to match

### Method Predicate
- `method` - HTTP methods to match (comma-separated)

### Header Predicate
- `header` - Header name
- `regexp` - Regular expression for header value

## Route Metadata Properties
- `timeout` - Route timeout in milliseconds
- `enabled` - Whether route is enabled
- `order` - Route priority order

## Property Naming Variations Identified

### Circuit Breaker Properties
**CamelCase Format:**
- `slidingWindowSize`
- `minimumNumberOfCalls`
- `failureRateThreshold`
- `waitDurationInOpenState`
- `permittedNumberOfCallsInHalfOpenState`
- `registerHealthIndicator`

**Kebab-Case Format:**
- `sliding-window-size`
- `minimum-number-of-calls`
- `failure-rate-threshold`
- `wait-duration-in-open-state`
- `permitted-number-of-calls-in-half-open-state`
- `register-health-indicator`
- `slow-call-rate-threshold`
- `slow-call-duration-threshold`

### HTTP Client Properties
**Nested Format:**
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
```

**Flat Format:**
```yaml
spring.cloud.gateway.httpclient:
  connect-timeout: 5000
```

## Configuration Value Variations

### Timeout Values
- **Connect Timeout:** 2000ms (test), 5000ms (standard)
- **Response Timeout:** 3s (test), 10s (standard), 30s (performance)
- **Circuit Breaker Timeout:** 2s (test), 3-5s (profiles)

### Circuit Breaker Parameters
- **Sliding Window Size:** 5 (test), 10 (standard), 15 (circuit-breaker profile), 20 (performance)
- **Minimum Calls:** 3 (test), 5 (standard), 8 (circuit-breaker profile), 10 (performance)
- **Failure Rate Threshold:** 50% (most), 55% (circuit-breaker profile for product service)
- **Wait Duration:** 5s (test/performance), 10s (standard), 12s (circuit-breaker profile)

### Management Endpoints
- **Standard:** health,info,circuitbreakers,circuitbreakerevents,refresh
- **Circuit Breaker Profile:** health,circuitbreakers,circuitbreakerevents
- **Performance Profile:** health,info,metrics,prometheus,performance