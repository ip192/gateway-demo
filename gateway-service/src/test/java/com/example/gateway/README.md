# Gateway Service Integration Tests

This directory contains comprehensive integration tests for the Gateway service that verify the end-to-end functionality of request routing and service communication.

## Test Files

### 1. GatewayIntegrationTest.java
**Purpose**: Tests the Gateway service in isolation with mocked downstream services.

**Test Cases**:
- `testGatewayApplicationStarts()`: Verifies the Gateway application starts successfully
- `testGatewayRouteConfiguration()`: Validates route configuration is loaded correctly
- `testUserServiceUnavailable_ShouldReturnError()`: Tests Gateway behavior when downstream service is unavailable
- `testInvalidRoute_ShouldReturn404()`: Verifies 404 response for non-existent routes
- `testGatewayHealthCheck()`: Tests the health check endpoint
- `testGatewayRouteMatching()`: Validates route matching logic

**Key Features**:
- Uses random ports to avoid conflicts
- Tests Gateway configuration and routing logic
- Verifies error handling when downstream services are unavailable
- Validates health check functionality

### 2. EndToEndIntegrationTest.java
**Purpose**: End-to-end integration tests that require both Gateway and User services to be running.

**Test Cases**:
- `testCompleteLoginFlow_ThroughGateway()`: Tests complete login flow through Gateway
- `testCompleteLogoutFlow_ThroughGateway()`: Tests complete logout flow through Gateway
- `testUserWorkflow_LoginThenLogout()`: Tests complete user workflow
- `testGatewayHealthCheck()`: Tests Gateway health endpoint
- `testConcurrentRequests_ThroughGateway()`: Tests concurrent request handling

**Key Features**:
- Only runs when system property `e2e.test=true` is set
- Uses fixed ports (Gateway: 8080, User: 8081)
- Requires both services to be running
- Tests actual service-to-service communication

## Running the Tests

### Unit/Integration Tests (Default)
```bash
mvn test
```
This runs `GatewayIntegrationTest` which tests the Gateway in isolation.

### End-to-End Tests
```bash
# First, start both services in separate terminals:
# Terminal 1: Start User service
cd user-service && mvn spring-boot:run

# Terminal 2: Start Gateway service  
cd gateway-service && mvn spring-boot:run

# Terminal 3: Run E2E tests
cd gateway-service && mvn test -De2e.test=true
```

## Test Coverage

The integration tests verify:

1. **Gateway Configuration**: Route definitions and predicates
2. **Request Routing**: Proper forwarding of requests to downstream services
3. **Error Handling**: Appropriate responses when services are unavailable
4. **Health Checks**: Service health monitoring
5. **Concurrent Processing**: Multiple simultaneous requests
6. **End-to-End Flows**: Complete user workflows through the Gateway

## Requirements Verification

These tests verify the following requirements from the specification:

- **需求 1.2**: Gateway can discover and connect to User service
- **需求 3.1**: Requests to /user/login are properly routed
- **需求 3.3**: Error handling when User service is unavailable  
- **需求 4.1**: Requests to /user/logout are properly routed
- **需求 4.3**: Error handling for logout requests
- **需求 5.4**: Health check endpoints are available

## Test Architecture

The tests use a layered approach:

1. **Unit Level**: Individual component testing (existing unit tests)
2. **Integration Level**: Gateway service with mocked dependencies (`GatewayIntegrationTest`)
3. **End-to-End Level**: Full system testing with real services (`EndToEndIntegrationTest`)

This ensures comprehensive coverage from individual components to complete system functionality.