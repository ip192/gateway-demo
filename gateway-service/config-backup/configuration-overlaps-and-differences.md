# Configuration Overlaps and Differences Summary

## Configuration File Overview
- **application.yml** - Main configuration with basic routes and circuit breakers
- **application-circuit-breaker.yml** - Enhanced circuit breaker configurations
- **application-dynamic-routing.yml** - Custom gateway configuration structure
- **application-performance.yml** - Performance monitoring and optimization
- **application-dynamic-routing-example.yml** - Extended routing examples
- **application-test.yml** - Test-optimized configurations

## Major Overlaps

### 1. Route Definitions
**Files with Similar Routes:** application.yml, application-circuit-breaker.yml, application-test.yml

**Common Routes:**
- user-service-routes (Path=/user/**)
- product-service-routes (Path=/product/**)

**Identical Configuration Elements:**
- Route URIs: http://localhost:8081, http://localhost:8082
- Path predicates: /user/**, /product/**
- CircuitBreaker filter with fallback URIs
- Retry filter with similar parameters
- Route metadata structure

### 2. Circuit Breaker Base Configuration
**Files:** application.yml, application-circuit-breaker.yml, application-test.yml

**Common Circuit Breaker Instances:**
- user-service-cb
- product-service-cb

**Shared Properties:**
- registerHealthIndicator: true
- Basic circuit breaker parameters (with different values)

### 3. HTTP Client Configuration
**Files:** application.yml, application-circuit-breaker.yml, application-performance.yml, application-test.yml

**Common Properties:**
- connect-timeout
- response-timeout

### 4. Management Health Configuration
**Files:** application.yml, application-circuit-breaker.yml, application-performance.yml

**Common Properties:**
- management.endpoint.health.show-details: always
- management.health.circuitbreakers.enabled: true

## Major Differences

### 1. Circuit Breaker Parameter Variations

#### User Service Circuit Breaker
| Property | application.yml | circuit-breaker.yml | performance.yml | test.yml |
|----------|----------------|-------------------|-----------------|----------|
| slidingWindowSize | 10 | 10 | 20 | 5 |
| minimumNumberOfCalls | 5 | 5 | 10 | 3 |
| waitDurationInOpenState | 10s | 10s | 5s | 5s |
| permittedNumberOfCallsInHalfOpenState | 3 | 3 | - | 2 |

#### Product Service Circuit Breaker
| Property | application.yml | circuit-breaker.yml | performance.yml | test.yml |
|----------|----------------|-------------------|-----------------|----------|
| slidingWindowSize | 10 | 15 | 20 | 5 |
| minimumNumberOfCalls | 5 | 8 | 10 | 3 |
| failureRateThreshold | 50 | 55 | 50 | 50 |
| waitDurationInOpenState | 10s | 12s | 5s | 5s |

### 2. Time Limiter Configurations

| Instance | application.yml | circuit-breaker.yml | test.yml |
|----------|----------------|-------------------|----------|
| user-service-cb | 5s | 3s | 2s |
| product-service-cb | 5s | 4s | 2s |

### 3. HTTP Client Timeout Variations

| Property | application.yml | circuit-breaker.yml | performance.yml | test.yml |
|----------|----------------|-------------------|-----------------|----------|
| connect-timeout | 5000ms | 5000ms | 5000ms | 2000ms |
| response-timeout | 10s | 10s | 30s | 3s |

### 4. Management Endpoints Exposure

| Profile | Exposed Endpoints |
|---------|------------------|
| application.yml | health,info,circuitbreakers,circuitbreakerevents,refresh |
| circuit-breaker.yml | health,circuitbreakers,circuitbreakerevents |
| performance.yml | health,info,metrics,prometheus,performance |

### 5. Unique Configuration Features

#### Performance Profile Only
- Connection pool configuration (max-connections: 500, max-idle-time: 30s)
- Metrics export to Prometheus
- Percentile histogram configuration
- Performance-optimized retry configurations
- Server compression settings
- Detailed logging configuration

#### Dynamic Routing Files Only
- Custom `gateway.routes` structure (non-Spring Cloud Gateway standard)
- Global circuit breaker configuration under `gateway.circuitBreaker`
- Global retry configuration under `gateway.retry`
- Additional route predicates (Method, Header)
- Additional filters (AddRequestHeader, AddResponseHeader, StripPrefix)
- Admin service route example

#### Test Profile Only
- Debug logging levels for gateway components
- Reduced timeouts for faster test execution
- Smaller circuit breaker windows for quicker testing

## Configuration Structure Differences

### 1. Route Configuration Formats

#### Standard Spring Cloud Gateway Format
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: route-id
          uri: target-uri
          predicates:
            - Path=/path/**
          filters:
            - name: FilterName
              args:
                parameter: value
```

#### Custom Gateway Format
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
```

### 2. Property Naming Inconsistencies

#### Circuit Breaker Properties
**CamelCase (application.yml, circuit-breaker.yml, test.yml):**
- slidingWindowSize
- minimumNumberOfCalls
- failureRateThreshold

**Kebab-Case (performance.yml):**
- sliding-window-size
- minimum-number-of-calls
- failure-rate-threshold

#### HTTP Client Properties
**Nested Format (application.yml, performance.yml):**
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 5000
```

**Flat Format (circuit-breaker.yml, test.yml):**
```yaml
spring.cloud.gateway.httpclient:
  connect-timeout: 5000
```

## Consolidation Recommendations

### 1. Eliminate Duplicate Route Definitions
- Consolidate user-service and product-service routes into main configuration
- Use profile-specific overrides only for parameters that actually differ
- Remove redundant route definitions across files

### 2. Standardize Property Naming
- Use consistent camelCase naming for all Resilience4j properties
- Standardize HTTP client configuration format (prefer nested format)
- Use consistent timeout format (prefer duration strings like "10s")

### 3. Profile-Specific Configurations
- **circuit-breaker profile**: Only enhanced circuit breaker parameters
- **performance profile**: Metrics, monitoring, and performance optimizations
- **dynamic-routing profile**: Custom gateway configuration examples
- **test profile**: Test-optimized timeouts and logging

### 4. Remove Configuration Conflicts
- Resolve different circuit breaker parameter values between profiles
- Standardize management endpoint exposure per profile purpose
- Consolidate HTTP client timeout configurations

## Properties Conversion Priority

### High Priority (Core Functionality)
1. Route definitions (user-service, product-service)
2. Circuit breaker configurations
3. HTTP client settings
4. Management endpoints

### Medium Priority (Profile-Specific)
1. Performance monitoring configurations
2. Enhanced circuit breaker parameters
3. Custom gateway routing structure

### Low Priority (Examples/Testing)
1. Dynamic routing examples
2. Test-specific configurations
3. Admin service route examples

## Identified Configuration Issues

### 1. Inconsistent Circuit Breaker Parameters
- Product service has different parameters in circuit-breaker profile
- Performance profile uses different property naming convention
- Test profile has significantly different values

### 2. HTTP Client Configuration Duplication
- Same timeout values repeated across multiple files
- Different property path formats used inconsistently

### 3. Management Endpoint Inconsistencies
- Different endpoint sets exposed per profile
- Some profiles missing important endpoints (info, refresh)

### 4. Route Definition Redundancy
- Nearly identical routes defined in multiple files
- Only circuit breaker parameters differ between some definitions