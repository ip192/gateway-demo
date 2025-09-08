# Info Routing Integration Tests

This document describes the integration tests for the custom info routing functionality in the Spring Cloud Gateway.

## Test Structure

The integration tests are organized into three main test classes:

### 1. InfoRoutingGatewayTest
**Purpose**: Tests the gateway's routing behavior and error handling without requiring backend services to be running.

**Key Features**:
- Tests info routing filter activation
- Validates error handling for missing/invalid parameters
- Verifies that existing routes are not affected by the info filter
- Tests filter ordering and integration with gateway infrastructure

**How to Run**:
```bash
mvn test -Dtest=InfoRoutingGatewayTest -f gateway-service/pom.xml
```

### 2. InfoRoutingCollaborationTest
**Purpose**: Tests the collaboration between InfoRoutingFilter and existing gateway components.

**Key Features**:
- Verifies InfoRoutingProperties configuration loading
- Tests integration with RouteLocator and other gateway beans
- Validates actuator endpoints remain accessible
- Tests concurrent request handling with mixed paths

**How to Run**:
```bash
mvn test -Dtest=InfoRoutingCollaborationTest -f gateway-service/pom.xml
```

### 3. InfoRoutingIntegrationTest
**Purpose**: End-to-end integration tests that require all services (gateway, user-service, product-service) to be running.

**Key Features**:
- Tests complete routing flow through the gateway
- Validates actual service responses
- Tests mixed workflows combining info routing and existing routes
- Requires system property `-De2e.test=true` to run

**How to Run**:
```bash
# First start all services
./start-services.sh

# Then run the end-to-end tests
mvn test -Dtest=InfoRoutingIntegrationTest -De2e.test=true -f gateway-service/pom.xml
```

## Test Coverage

### Error Scenarios Tested
- Missing `id` parameter in request body
- Invalid `id` prefix (not matching user- or product-)
- Empty request body
- Invalid JSON format
- Unsupported HTTP methods (GET instead of POST)

### Routing Scenarios Tested
- User service routing (id starting with "user-")
- Product service routing (id starting with "product-")
- Existing route preservation (/user/login, /product/add, etc.)
- Concurrent request handling
- Mixed workflow scenarios

### Configuration Testing
- InfoRoutingProperties loading and validation
- Service URL configuration
- Prefix configuration
- Filter ordering and priority

## Running All Integration Tests

To run all info routing integration tests:

```bash
# Run gateway-only tests (no backend services required)
mvn test -Dtest="InfoRoutingGatewayTest,InfoRoutingCollaborationTest" -f gateway-service/pom.xml

# Run end-to-end tests (requires all services running)
./start-services.sh
mvn test -Dtest=InfoRoutingIntegrationTest -De2e.test=true -f gateway-service/pom.xml
./stop-services.sh
```

## Test Configuration

The tests use the following configuration properties:

```properties
# Enable info routing
gateway.info-routing.enabled=true

# Configure prefixes
gateway.info-routing.prefixes.user=user-
gateway.info-routing.prefixes.product=product-

# Configure service URLs
gateway.info-routing.services.user-service.url=http://localhost:8081
gateway.info-routing.services.product-service.url=http://localhost:8082

# Configure existing routes for testing
spring.cloud.gateway.routes[0].id=user-service-test
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**

spring.cloud.gateway.routes[1].id=product-service-test
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**
```

## Expected Test Results

### When Backend Services Are Not Running
- Gateway-only tests should pass completely
- Tests verify proper error handling and filter behavior
- Connection refused errors are expected and handled gracefully

### When All Services Are Running
- All tests should pass including end-to-end scenarios
- Actual service responses are validated
- Complete routing workflows are tested

## Troubleshooting

### Common Issues

1. **Tests fail with "Connection refused"**
   - This is expected for gateway-only tests when backend services are not running
   - For end-to-end tests, ensure all services are started with `./start-services.sh`

2. **Port conflicts**
   - Tests use random ports for the gateway
   - Backend services should run on their configured ports (8081, 8082)

3. **Configuration not loaded**
   - Verify test properties are correctly set in `@TestPropertySource`
   - Check that InfoRoutingConfiguration is properly loaded

### Debug Information

To enable debug logging for tests:

```bash
mvn test -Dtest=InfoRoutingGatewayTest -Dlogging.level.com.example.gateway=DEBUG -f gateway-service/pom.xml
```

## Requirements Verification

These integration tests verify the following requirements from the specification:

- **Requirement 1.1-1.4**: Parameter-based routing functionality
- **Requirement 2.1-2.3**: Existing route preservation
- **Requirement 3.1-3.3**: WebFilter integration
- **Requirement 4.1-4.4**: Error handling and responses
- **Requirement 5.1-5.4**: Configuration management

The tests ensure that the info routing feature works correctly in isolation and integrates properly with the existing Spring Cloud Gateway infrastructure.