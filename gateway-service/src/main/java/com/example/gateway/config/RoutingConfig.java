package com.example.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 路由配置类
 * 负责动态路由的基本配置，与servlet-based的DynamicProxyController协作
 */
@Configuration
@ConditionalOnProperty(name = "gateway.dynamic-routing.enabled", havingValue = "true", matchIfMissing = true)
public class RoutingConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RoutingConfig.class);
    
    @Autowired
    private DynamicRoutingProperties dynamicRoutingProperties;
    
    /**
     * 检查动态路由是否启用
     * 
     * @return 是否启用动态路由
     */
    public boolean isDynamicRoutingEnabled() {
        boolean enabled = dynamicRoutingProperties != null && dynamicRoutingProperties.isEnabled();
        logger.debug("动态路由启用状态: {}", enabled);
        return enabled;
    }
    
    /**
     * 获取动态路由配置属性
     * 
     * @return 动态路由配置属性
     */
    public DynamicRoutingProperties getDynamicRoutingProperties() {
        return dynamicRoutingProperties;
    }
    
    /**
     * 检查路径是否应该使用动态路由
     * 
     * @param path 请求路径
     * @param serviceName 目标服务名称
     * @return 是否使用动态路由
     */
    public boolean shouldUseDynamicRouting(String path, String serviceName) {
        // 检查动态路由是否启用
        if (!isDynamicRoutingEnabled()) {
            logger.debug("动态路由已禁用，跳过: {}", path);
            return false;
        }
        
        // 检查是否是已知的静态路由路径
        if (isStaticRoutePath(path, serviceName)) {
            logger.debug("请求匹配静态路由，优先使用静态路由: {}", path);
            return false;
        }
        
        logger.debug("使用动态路由处理请求: {} -> {}", path, serviceName);
        return true;
    }
    
    /**
     * 检查路径是否为已知的静态路由路径
     * 
     * @param path 请求路径
     * @param serviceName 服务名称
     * @return 是否为静态路由路径
     */
    private boolean isStaticRoutePath(String path, String serviceName) {
        // 这里可以定义已知的静态路由路径
        // 目前所有/user/**和/product/**都通过动态路由处理
        // 如果将来有特定的静态路由，可以在这里添加判断逻辑
        
        if ("user-service".equals(serviceName)) {
            // 检查是否有特定的用户服务静态路由
            // 例如: /user/static/** 等特殊路径
            return path.startsWith("/user/static/");
        }
        
        if ("product-service".equals(serviceName)) {
            // 检查是否有特定的产品服务静态路由
            // 例如: /product/static/** 等特殊路径
            return path.startsWith("/product/static/");
        }
        
        return false;
    }
}