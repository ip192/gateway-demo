# Design Document

## Overview

This design outlines the conversion of gateway-service configuration files from YAML format to properties format. The conversion will consolidate multiple YAML files into organized properties files while maintaining all existing functionality. The design focuses on preserving configuration semantics, improving maintainability, and following Spring Boot properties conventions.

## Architecture

### Current State
- Multiple YAML configuration files:
  - `application.yml` (main configuration)
  - `application-dynamic-routing.yml` (dynamic routing example)
  - `application-circuit-breaker.yml` (circuit breaker focused)
  - `application-performance.yml` (performance monitoring)
- Mixed configuration approaches with some duplication
- Existing `application.properties` with basic configuration

### Target State
- Consolidated properties files organized by profile:
  - `application.properties` (main configuration)
  - `application-circuit-breaker.properties` (circuit breaker profile)
  - `application-performance.properties` (performance profile)
  - `application-dynamic-routing.properties` (dynamic routing profile)
- Consistent properties naming conventions
- Elimination of configuration duplication

## Components and Interfaces

### Configuration File Structure

#### Main Configuration (`application.properties`)
- Server configuration (port, compression)
- Spring application settings
- Basic gateway routes configuration
- Default HTTP client settings
- Standard management endpoints

#### Circuit Breaker Profile (`application-circuit-breaker.properties`)
- Enhanced circuit breaker configurations
- Resilience4j specific settings
- Time limiter configurations
- Circuit breaker monitoring endpoints

#### Performance Profile (`application-performance.properties`)
- Performance optimized settings
- Metrics and monitoring configuration
- Connection pool settings
- Prometheus export configuration
- Performance-specific route configurations

#### Dynamic Routing Profile (`application-dynamic-routing.properties`)
- Custom gateway routing configuration structure
- Route metadata management
- Dynamic route examples

### Properties Naming Conventions

#### Spring Cloud Gateway Routes
```properties
# Route definition pattern
spring.cloud.gateway.routes[index].id=route-id
spring.cloud.gateway.routes[index].uri=target-uri
spring.cloud.gateway.routes[index].predicates[index]=predicate-definition
spring.cloud.gateway.routes[index].filters[index].name=filter-name
spring.cloud.gateway.routes[index].filters[index].args.property=value
spring.cloud.gateway.routes[index].metadata.property=value
```

#### Resilience4j Configuration
```properties
# Circuit breaker pattern
resilience4j.circuitbreaker.instances.instance-name.property=value
resilience4j.timelimiter.instances.instance-name.property=value
resilience4j.retry.instances.instance-name.property=value
```

#### Management Configuration
```properties
# Management endpoints pattern
management.endpoints.web.exposure.include=endpoint-list
management.endpoint.endpoint-name.property=value
management.health.component.enabled=boolean
management.metrics.property=value
```

## Data Models

### Route Configuration Model
```properties
# User Service Route
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

### Circuit Breaker Configuration Model
```properties
# Circuit Breaker Instance
resilience4j.circuitbreaker.instances.service-name-cb.slidingWindowSize=10
resilience4j.circuitbreaker.instances.service-name-cb.minimumNumberOfCalls=5
resilience4j.circuitbreaker.instances.service-name-cb.failureRateThreshold=50
resilience4j.circuitbreaker.instances.service-name-cb.waitDurationInOpenState=10s
resilience4j.circuitbreaker.instances.service-name-cb.permittedNumberOfCallsInHalfOpenState=3
resilience4j.circuitbreaker.instances.service-name-cb.registerHealthIndicator=true
```

### HTTP Client Configuration Model
```properties
# HTTP Client Settings
spring.cloud.gateway.httpclient.connect-timeout=5000
spring.cloud.gateway.httpclient.response-timeout=10s
spring.cloud.gateway.httpclient.pool.max-connections=500
spring.cloud.gateway.httpclient.pool.max-idle-time=30s
```

## Error Handling

### Configuration Validation
- Properties syntax validation during application startup
- Route configuration validation using existing validators
- Circuit breaker parameter validation
- HTTP client timeout validation

### Migration Safety
- Backup existing YAML files before conversion
- Gradual migration approach with profile-based testing
- Configuration comparison testing to ensure equivalence
- Rollback capability by maintaining YAML files temporarily

### Runtime Error Handling
- Maintain existing error handling mechanisms
- Preserve fallback controller functionality
- Keep circuit breaker error handling intact
- Maintain retry mechanism error handling

## Testing Strategy

### Configuration Equivalence Testing
- Unit tests to verify properties loading matches YAML configuration
- Integration tests to ensure route behavior is identical
- Circuit breaker functionality tests with properties configuration
- Performance tests to validate HTTP client settings

### Profile-Specific Testing
- Test each profile configuration independently
- Verify profile combination scenarios
- Test configuration override behavior
- Validate environment-specific property resolution

### Migration Testing
- Before/after comparison tests
- Configuration parsing tests
- Application startup tests with new properties
- End-to-end functionality tests

### Test Coverage Areas
1. **Route Configuration Tests**
   - Path predicate matching
   - Filter application and ordering
   - Metadata preservation
   - URI resolution

2. **Circuit Breaker Tests**
   - Instance configuration loading
   - Health indicator registration
   - Timeout configuration
   - Fallback behavior

3. **Management Endpoint Tests**
   - Endpoint exposure verification
   - Health check detail levels
   - Metrics collection
   - Prometheus export functionality

4. **HTTP Client Tests**
   - Connection timeout behavior
   - Response timeout handling
   - Connection pool configuration
   - Performance characteristics

### Conversion Process
1. **Analysis Phase**: Compare existing YAML configurations and identify all unique settings
2. **Mapping Phase**: Create properties equivalents for all YAML configurations
3. **Consolidation Phase**: Organize properties into logical profile-based files
4. **Validation Phase**: Test configuration loading and application behavior
5. **Migration Phase**: Replace YAML files with properties files
6. **Cleanup Phase**: Remove redundant YAML files after successful validation