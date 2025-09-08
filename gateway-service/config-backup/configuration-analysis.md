# Gateway Service Configuration Analysis

## Overview
This document provides a comprehensive analysis of all YAML configuration files in the gateway-service, documenting unique properties, overlaps, and differences between files.

## Backup Files Created
- `application.yml.backup` - Main configuration file
- `application-circuit-breaker.yml.backup` - Circuit breaker focused configuration
- `application-dynamic-routing.yml.backup` - Dynamic routing configuration structure
- `application-performance.yml.backup` - Performance monitoring configuration
- `application-dynamic-routing-example.yml.backup` - Dynamic routing examples
- `application-test.yml.backup` - Test configuration

## Configuration Properties Analysis

### 1. Server Configuration
**Files:** application.yml, application-performance.yml

#### application.yml
```yaml
server:
  port: 8080
```

#### application-performance.yml
```yaml
server:
  port: 8080
  netty:
    connection-timeout: 5s
    h2c-max-content-length: 0B
  compression:
    enabled: true
    mime-types: application/json,text/plain,text/html
```

**Differences:** Performance profile adds Netty-specific settings and compression configuration.

### 2. Spring Application Configuration
**Files:** application.yml, application-performance.yml

#### application.yml
```yaml
spring:
  application:
    name: gateway-service
```

#### application-performance.yml
```yaml
spring:
  application:
    name: gateway-service
```

**Overlap:** Identical configuration in both files.

### 3. Spring Cloud Gateway Routes Configuration

#### Standard Routes (application.yml, application-circuit-breaker.yml, application-test.yml)
**Common Structure:**
- Route IDs: user-service-routes, product-service-routes
- URIs: http://localhost:8081, http://localhost:8082
- Predicates: Path=/user/**, Path=/product/**
- Filters: CircuitBreaker, Retry
- Metadata: timeout, enabled, order

**Key Differences:**
- **application-circuit-breaker.yml**: Different circuit breaker parameters for product-service-cb
- **application-test.yml**: Reduced sliding window sizes and timeouts for testing

#### Custom Gateway Routes (application-dynamic-routing.yml, application-performance.yml, application-dynamic-routing-example.yml)
**Structure:**
```yaml
gateway:
  routes:
    - id: route-id
      uri: target-uri
      predicates:
        - name: PredicateName
          args:
            parameter: value
      filters:
        - name: FilterName
          args:
            parameter: value
      metadata:
        property: value
```

**Unique Features:**
- **application-dynamic-routing-example.yml**: Additional admin-service route, Method predicates, Header predicates, AddRequestHeader/AddResponseHeader filters, StripPrefix filter
- **application-performance.yml**: Optimized retry configurations with reduced backoff times

### 4. HTTP Client Configuration

#### application.yml
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
        response-timeout: 10s
```

#### application-circuit-breaker.yml
```yaml
spring.cloud.gateway.httpclient:
  connect-timeout: 5000
  response-timeout: 10s
```

#### application-performance.yml
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500
          max-idle-time: 30s
        connect-timeout: 5000
        response-timeout: 30s
```

#### application-test.yml
```yaml
spring.cloud.gateway.httpclient:
  connect-timeout: 2000
  response-timeout: 3s
```

**Differences:**
- Performance profile adds connection pool configuration and longer response timeout
- Test profile uses shorter timeouts for faster test execution
- Different property path formats (nested vs. flat)

### 5. Resilience4j Circuit Breaker Configuration

#### Standard Configuration (application.yml)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service-cb:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true
      product-service-cb:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true
```

#### Circuit Breaker Profile (application-circuit-breaker.yml)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service-cb: # Same as application.yml
      product-service-cb:
        slidingWindowSize: 15        # Different: 15 vs 10
        minimumNumberOfCalls: 8      # Different: 8 vs 5
        failureRateThreshold: 55     # Different: 55 vs 50
        waitDurationInOpenState: 12s # Different: 12s vs 10s
        permittedNumberOfCallsInHalfOpenState: 3
        registerHealthIndicator: true
```

#### Performance Profile (application-performance.yml)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service-cb:
        failure-rate-threshold: 50           # Different property name format
        wait-duration-in-open-state: 5s     # Different: 5s vs 10s
        sliding-window-size: 20              # Different: 20 vs 10
        minimum-number-of-calls: 10          # Different: 10 vs 5
        slow-call-rate-threshold: 50         # Additional property
        slow-call-duration-threshold: 2s     # Additional property
```

#### Test Configuration (application-test.yml)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service-cb:
        slidingWindowSize: 5         # Different: 5 vs 10
        minimumNumberOfCalls: 3      # Different: 3 vs 5
        waitDurationInOpenState: 5s  # Different: 5s vs 10s
        permittedNumberOfCallsInHalfOpenState: 2  # Different: 2 vs 3
```

### 6. Time Limiter Configuration

#### Standard (application.yml, application-circuit-breaker.yml)
```yaml
resilience4j:
  timelimiter:
    instances:
      user-service-cb:
        timeoutDuration: 5s  # application.yml
        timeoutDuration: 3s  # application-circuit-breaker.yml
      product-service-cb:
        timeoutDuration: 5s  # application.yml
        timeoutDuration: 4s  # application-circuit-breaker.yml
```

#### Test Configuration
```yaml
resilience4j:
  timelimiter:
    instances:
      user-service-cb:
        timeoutDuration: 2s
      product-service-cb:
        timeoutDuration: 2s
```

### 7. Retry Configuration

#### Performance Profile (application-performance.yml)
```yaml
resilience4j:
  retry:
    instances:
      user-service-retry:
        max-attempts: 3
        wait-duration: 10ms
        exponential-backoff-multiplier: 2
      product-service-retry:
        max-attempts: 3
        wait-duration: 10ms
        exponential-backoff-multiplier: 2
```

**Unique:** Only present in performance profile as separate retry instances.

### 8. Management Endpoints Configuration

#### application.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers,circuitbreakerevents,refresh
  endpoint:
    health:
      show-details: always
    refresh:
      enabled: true
  health:
    circuitbreakers:
      enabled: true
```

#### application-circuit-breaker.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,circuitbreakers,circuitbreakerevents  # Missing info,refresh
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

#### application-performance.yml
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,performance  # Different endpoints
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true  # Additional endpoint configuration
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
        gateway.request.processing.time: true
      percentiles:
        http.server.requests: 0.5,0.95,0.99
        gateway.request.processing.time: 0.5,0.95,0.99
    tags:
      application: gateway-service
      environment: performance-test
```

### 9. Gateway Metrics Configuration

#### application-performance.yml (Unique)
```yaml
spring:
  cloud:
    gateway:
      metrics:
        enabled: true
```

### 10. Logging Configuration

#### application-performance.yml (Unique)
```yaml
logging:
  level:
    com.example.gateway: INFO
    org.springframework.cloud.gateway: WARN
    reactor.netty: WARN
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

#### application-test.yml (Unique)
```yaml
logging:
  level:
    com.example.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
    io.github.resilience4j: DEBUG
```

### 11. Custom Gateway Configuration

#### application-dynamic-routing.yml, application-dynamic-routing-example.yml
```yaml
gateway:
  circuitBreaker:
    failureRateThreshold: 50
    waitDurationInOpenState: 10000
    slidingWindowSize: 10
    minimumNumberOfCalls: 5
  retry:
    retries: 3
    firstBackoff: 50
    maxBackoff: 500
    factor: 2.0
```

**Unique:** Global circuit breaker and retry configuration structure.

## Configuration Overlaps and Conflicts

### 1. Route Configuration Overlaps
- **application.yml** and **application-circuit-breaker.yml**: Nearly identical routes with different circuit breaker parameters
- **application-test.yml**: Similar routes with test-optimized parameters
- **Custom gateway routes**: Different structure but similar functionality

### 2. Circuit Breaker Configuration Conflicts
- **Property naming**: Some files use camelCase (`slidingWindowSize`), others use kebab-case (`sliding-window-size`)
- **Parameter values**: Different timeout and threshold values across profiles
- **Structure**: Standard Resilience4j vs. custom gateway configuration

### 3. HTTP Client Configuration Variations
- **Property paths**: Nested (`spring.cloud.gateway.httpclient`) vs. flat (`spring.cloud.gateway.httpclient.connect-timeout`)
- **Timeout values**: Different values for different environments
- **Pool configuration**: Only present in performance profile

### 4. Management Endpoints Differences
- **Exposed endpoints**: Different sets of endpoints per profile
- **Metrics configuration**: Comprehensive in performance profile, absent in others

## Recommendations for Properties Conversion

### 1. Consolidation Strategy
- **Main application.properties**: Core server, application, and basic gateway configuration
- **Profile-specific properties**: Circuit breaker, performance, and dynamic routing profiles
- **Test properties**: Separate test configuration file

### 2. Property Naming Standardization
- Use consistent camelCase naming for all properties
- Standardize timeout formats (prefer duration strings like "10s")
- Use indexed array notation for routes: `spring.cloud.gateway.routes[0].id`

### 3. Configuration Deduplication
- Remove duplicate route definitions across files
- Consolidate common circuit breaker parameters
- Standardize HTTP client configuration format

### 4. Profile Organization
- **circuit-breaker profile**: Enhanced circuit breaker and time limiter configurations
- **performance profile**: Metrics, monitoring, and performance-optimized settings
- **dynamic-routing profile**: Custom gateway configuration structure examples

## Properties Conversion Mapping

### Route Configuration
```properties
# Standard Spring Cloud Gateway format
spring.cloud.gateway.routes[0].id=user-service-routes
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**
spring.cloud.gateway.routes[0].filters[0].name=CircuitBreaker
spring.cloud.gateway.routes[0].filters[0].args.name=user-service-cb
spring.cloud.gateway.routes[0].filters[0].args.fallbackUri=forward:/fallback/user
spring.cloud.gateway.routes[0].metadata.timeout=5000
spring.cloud.gateway.routes[0].metadata.enabled=true
spring.cloud.gateway.routes[0].metadata.order=1
```

### Circuit Breaker Configuration
```properties
resilience4j.circuitbreaker.instances.user-service-cb.slidingWindowSize=10
resilience4j.circuitbreaker.instances.user-service-cb.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.user-service-cb.failureRateThreshold=50
resilience4j.circuitbreaker.instances.user-service-cb.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.user-service-cb.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.user-service-cb.registerHealthIndicator=true
```

### HTTP Client Configuration
```properties
spring.cloud.gateway.httpclient.connect-timeout=5000
spring.cloud.gateway.httpclient.response-timeout=10s
spring.cloud.gateway.httpclient.pool.max-connections=500
spring.cloud.gateway.httpclient.pool.max-idle-time=30s
```

### Management Configuration
```properties
management.endpoints.web.exposure.include=health,info,circuitbreakers,circuitbreakerevents,refresh
management.endpoint.health.show-details=always
management.endpoint.refresh.enabled=true
management.health.circuitbreakers.enabled=true
```