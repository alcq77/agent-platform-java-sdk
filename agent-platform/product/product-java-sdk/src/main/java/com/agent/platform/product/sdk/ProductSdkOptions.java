package com.agent.platform.product.sdk;

import com.agent.platform.product.core.model.RoutePolicy;
import com.agent.platform.product.spi.model.ProductEndpointConfig;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ProductSdkOptions {

    private String logicalModel = "primary-llm";

    private int maxHistoryMessages = 40;

    private Map<String, ProductEndpointConfig> endpoints = new LinkedHashMap<>();

    private Map<String, String> routing = new LinkedHashMap<>();

    private Map<String, RoutePolicy> routePolicies = new LinkedHashMap<>();

    /**
     * tool-calling 最大回合数，防止模型与工具进入无限循环。
     */
    private int maxToolCallIterations = 3;

    /**
     * 是否启用端点熔断。
     */
    private boolean circuitBreakerEnabled = true;

    /**
     * 连续失败达到阈值后打开熔断。
     */
    private int circuitFailureThreshold = 3;

    /**
     * 熔断打开后冷却秒数。
     */
    private int circuitOpenSeconds = 30;
}
