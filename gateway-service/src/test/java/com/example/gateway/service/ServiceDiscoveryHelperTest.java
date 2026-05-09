package com.example.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceDiscoveryHelperTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private LoadBalancerClient loadBalancerClient;

    @Mock
    private ServiceInstance serviceInstance;

    private ServiceDiscoveryHelper serviceDiscoveryHelper;

    @BeforeEach
    void setUp() {
        serviceDiscoveryHelper = new ServiceDiscoveryHelper(discoveryClient, loadBalancerClient);
    }

    @Test
    void testGetServiceInstance_Success() {
        // Given
        String serviceName = "user-service";
        List<ServiceInstance> instances = Arrays.asList(serviceInstance);
        
        when(discoveryClient.getInstances(serviceName)).thenReturn(instances);
        when(loadBalancerClient.choose(serviceName)).thenReturn(serviceInstance);

        // When
        ServiceInstance result = serviceDiscoveryHelper.getServiceInstance(serviceName);

        // Then
        assertNotNull(result);
        assertEquals(serviceInstance, result);
        verify(discoveryClient).getInstances(serviceName);
        verify(loadBalancerClient).choose(serviceName);
    }

    @Test
    void testGetServiceInstance_NoInstancesRegistered() {
        // Given
        String serviceName = "user-service";
        when(discoveryClient.getInstances(serviceName)).thenReturn(Collections.emptyList());

        // When & Then
        ServiceDiscoveryException exception = assertThrows(ServiceDiscoveryException.class, 
            () -> serviceDiscoveryHelper.getServiceInstance(serviceName));
        
        assertTrue(exception.getMessage().contains("服务未注册或不可用"));
        verify(discoveryClient).getInstances(serviceName);
        verify(loadBalancerClient, never()).choose(serviceName);
    }

    @Test
    void testGetServiceInstance_LoadBalancerReturnsNull() {
        // Given
        String serviceName = "user-service";
        List<ServiceInstance> instances = Arrays.asList(serviceInstance);
        
        when(discoveryClient.getInstances(serviceName)).thenReturn(instances);
        when(loadBalancerClient.choose(serviceName)).thenReturn(null);

        // When & Then
        ServiceDiscoveryException exception = assertThrows(ServiceDiscoveryException.class, 
            () -> serviceDiscoveryHelper.getServiceInstance(serviceName));
        
        assertTrue(exception.getMessage().contains("没有可用的健康服务实例"));
        verify(discoveryClient).getInstances(serviceName);
        verify(loadBalancerClient).choose(serviceName);
    }

    @Test
    void testBuildServiceUrl_Success() {
        // Given
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8081);
        when(serviceInstance.getServiceId()).thenReturn("user-service");
        String path = "/user/info";

        // When
        String result = serviceDiscoveryHelper.buildServiceUrl(serviceInstance, path);

        // Then
        assertEquals("http://localhost:8081/user/info", result);
    }

    @Test
    void testBuildServiceUrl_WithoutLeadingSlash() {
        // Given
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8081);
        when(serviceInstance.getServiceId()).thenReturn("user-service");
        String path = "user/info";

        // When
        String result = serviceDiscoveryHelper.buildServiceUrl(serviceInstance, path);

        // Then
        assertEquals("http://localhost:8081/user/info", result);
    }

    @Test
    void testBuildServiceUrl_NullPath() {
        // Given
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8081);
        when(serviceInstance.getServiceId()).thenReturn("user-service");

        // When
        String result = serviceDiscoveryHelper.buildServiceUrl(serviceInstance, null);

        // Then
        assertEquals("http://localhost:8081/", result);
    }

    @Test
    void testBuildServiceUrl_NullInstance() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> serviceDiscoveryHelper.buildServiceUrl(null, "/path"));
        
        assertEquals("服务实例不能为空", exception.getMessage());
    }

    @Test
    void testIsServiceAvailable_True() {
        // Given
        String serviceName = "user-service";
        List<ServiceInstance> instances = Arrays.asList(serviceInstance);
        when(discoveryClient.getInstances(serviceName)).thenReturn(instances);

        // When
        boolean result = serviceDiscoveryHelper.isServiceAvailable(serviceName);

        // Then
        assertTrue(result);
        verify(discoveryClient).getInstances(serviceName);
    }

    @Test
    void testIsServiceAvailable_False() {
        // Given
        String serviceName = "user-service";
        when(discoveryClient.getInstances(serviceName)).thenReturn(Collections.emptyList());

        // When
        boolean result = serviceDiscoveryHelper.isServiceAvailable(serviceName);

        // Then
        assertFalse(result);
        verify(discoveryClient).getInstances(serviceName);
    }

    @Test
    void testGetRegisteredServices_Success() {
        // Given
        List<String> services = Arrays.asList("user-service", "product-service");
        when(discoveryClient.getServices()).thenReturn(services);

        // When
        List<String> result = serviceDiscoveryHelper.getRegisteredServices();

        // Then
        assertEquals(services, result);
        verify(discoveryClient).getServices();
    }
}