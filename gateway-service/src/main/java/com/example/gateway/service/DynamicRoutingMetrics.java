package com.example.gateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 动态路由监控指标组件
 * 负责记录动态路由的成功、失败次数和响应时间等监控指标
 */
@Component
public class DynamicRoutingMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter routingSuccessCounter;
    private final Counter routingFailureCounter;
    private final Timer routingTimer;
    
    @Autowired
    public DynamicRoutingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化计数器和计时器
        this.routingSuccessCounter = Counter.builder("gateway.dynamic.routing.success")
                .description("动态路由成功次数")
                .register(meterRegistry);
                
        this.routingFailureCounter = Counter.builder("gateway.dynamic.routing.failure")
                .description("动态路由失败次数")
                .register(meterRegistry);
                
        this.routingTimer = Timer.builder("gateway.dynamic.routing.duration")
                .description("动态路由响应时间")
                .register(meterRegistry);
    }
    
    /**
     * 记录成功的路由请求
     * 
     * @param serviceName 目标服务名称
     * @param httpMethod HTTP方法
     * @param statusCode 响应状态码
     */
    public void recordSuccess(String serviceName, String httpMethod, int statusCode) {
        if (meterRegistry != null) {
            Counter.builder("gateway.dynamic.routing.success")
                    .description("动态路由成功次数")
                    .tags("service", serviceName, "method", httpMethod, "status", String.valueOf(statusCode))
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 记录失败的路由请求
     * 
     * @param serviceName 目标服务名称
     * @param httpMethod HTTP方法
     * @param errorType 错误类型
     */
    public void recordFailure(String serviceName, String httpMethod, String errorType) {
        if (meterRegistry != null) {
            Counter.builder("gateway.dynamic.routing.failure")
                    .description("动态路由失败次数")
                    .tags("service", serviceName, "method", httpMethod, "error", errorType)
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 开始计时
     * 
     * @return Timer.Sample 计时器样本
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 停止计时并记录
     * 
     * @param sample 计时器样本
     * @param serviceName 目标服务名称
     * @param httpMethod HTTP方法
     */
    public void stopTimer(Timer.Sample sample, String serviceName, String httpMethod) {
        if (meterRegistry != null && sample != null) {
            sample.stop(Timer.builder("gateway.dynamic.routing.duration")
                    .tags("service", serviceName, "method", httpMethod)
                    .register(meterRegistry));
        }
    }
    
    /**
     * 记录服务发现失败
     * 
     * @param serviceName 服务名称
     */
    public void recordServiceDiscoveryFailure(String serviceName) {
        if (meterRegistry != null) {
            Counter.builder("gateway.service.discovery.failure")
                    .description("服务发现失败次数")
                    .tags("service", serviceName)
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 记录HTTP代理失败
     * 
     * @param serviceName 服务名称
     * @param errorType 错误类型
     */
    public void recordProxyFailure(String serviceName, String errorType) {
        if (meterRegistry != null) {
            Counter.builder("gateway.proxy.failure")
                    .description("HTTP代理失败次数")
                    .tags("service", serviceName, "error", errorType)
                    .register(meterRegistry)
                    .increment();
        }
    }
    
    /**
     * 获取MeterRegistry实例
     * 
     * @return MeterRegistry实例
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}