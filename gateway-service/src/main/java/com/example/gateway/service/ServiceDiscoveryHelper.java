package com.example.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务发现工具类，封装服务发现和负载均衡逻辑
 */
@Component
public class ServiceDiscoveryHelper {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryHelper.class);
    
    private final DiscoveryClient discoveryClient;
    private final LoadBalancerClient loadBalancerClient;
    
    public ServiceDiscoveryHelper(DiscoveryClient discoveryClient, LoadBalancerClient loadBalancerClient) {
        this.discoveryClient = discoveryClient;
        this.loadBalancerClient = loadBalancerClient;
    }
    
    /**
     * 获取健康的服务实例
     * 
     * @param serviceName 服务名称
     * @return 服务实例，如果没有可用实例则抛出异常
     * @throws ServiceDiscoveryException 当服务不可用时抛出
     */
    public ServiceInstance getServiceInstance(String serviceName) {
        logger.debug("正在查找服务实例: {}", serviceName);
        
        try {
            // 首先检查服务是否已注册
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            if (instances == null || instances.isEmpty()) {
                logger.warn("未找到服务实例: {}", serviceName);
                throw new ServiceDiscoveryException("服务未注册或不可用: " + serviceName);
            }
            
            // 使用负载均衡器选择一个健康的实例
            ServiceInstance instance = loadBalancerClient.choose(serviceName);
            if (instance == null) {
                logger.warn("负载均衡器未能选择到健康的服务实例: {}", serviceName);
                throw new ServiceDiscoveryException("没有可用的健康服务实例: " + serviceName);
            }
            
            logger.debug("成功获取服务实例: {} -> {}:{}", serviceName, instance.getHost(), instance.getPort());
            return instance;
            
        } catch (Exception e) {
            if (e instanceof ServiceDiscoveryException) {
                throw e;
            }
            logger.error("服务发现过程中发生异常: {}", serviceName, e);
            throw new ServiceDiscoveryException("服务发现失败: " + serviceName + ", 原因: " + e.getMessage());
        }
    }
    
    /**
     * 构建目标服务URL
     * 
     * @param instance 服务实例
     * @param path 请求路径
     * @return 完整的服务URL
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public String buildServiceUrl(ServiceInstance instance, String path) {
        if (instance == null) {
            throw new IllegalArgumentException("服务实例不能为空");
        }
        
        if (path == null) {
            path = "";
        }
        
        // 确保路径以 / 开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        String url = String.format("http://%s:%d%s", 
                                 instance.getHost(), 
                                 instance.getPort(), 
                                 path);
        
        logger.debug("构建服务URL: {} -> {}", instance.getServiceId(), url);
        return url;
    }
    
    /**
     * 检查服务是否可用
     * 
     * @param serviceName 服务名称
     * @return 如果服务可用返回true，否则返回false
     */
    public boolean isServiceAvailable(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            boolean available = instances != null && !instances.isEmpty();
            logger.debug("服务可用性检查: {} -> {}", serviceName, available);
            return available;
        } catch (Exception e) {
            logger.warn("检查服务可用性时发生异常: {}", serviceName, e);
            return false;
        }
    }
    
    /**
     * 获取所有已注册的服务名称
     * 
     * @return 服务名称列表
     */
    public List<String> getRegisteredServices() {
        try {
            List<String> services = discoveryClient.getServices();
            logger.debug("已注册的服务: {}", services);
            return services;
        } catch (Exception e) {
            logger.error("获取已注册服务列表时发生异常", e);
            throw new ServiceDiscoveryException("无法获取已注册服务列表: " + e.getMessage());
        }
    }
}