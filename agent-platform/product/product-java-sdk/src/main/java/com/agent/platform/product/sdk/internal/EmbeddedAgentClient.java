package com.agent.platform.product.sdk.internal;

import com.agent.platform.agent.api.dto.AgentChatRequest;
import com.agent.platform.agent.api.dto.AgentChatResponse;
import com.agent.platform.product.core.agent.ProductAgentEngine;
import com.agent.platform.product.core.model.ProductModelRouter;
import com.agent.platform.product.sdk.AgentClient;
import com.agent.platform.product.sdk.ProductSdkOptions;
import com.agent.platform.product.spi.model.ProductEndpointConfig;
import com.agent.platform.product.spi.model.ProductModelProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EmbeddedAgentClient implements AgentClient, CircuitBreakerSnapshotProvider {

    private final ProductSdkOptions options;
    private final ProductModelRouter modelRouter;
    private final ProductAgentEngine agentEngine;
    private final Map<String, ProductModelProvider> providers;
    private final Map<String, EndpointCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public EmbeddedAgentClient(ProductSdkOptions options,
                               ProductModelRouter modelRouter,
                               ProductAgentEngine agentEngine,
                               Map<String, ProductModelProvider> providers) {
        this.options = options;
        this.modelRouter = modelRouter;
        this.agentEngine = agentEngine;
        this.providers = providers;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        String logicalModel = options.getLogicalModel();
        List<ProductEndpointConfig> candidates = modelRouter.resolveCandidates(
                logicalModel, options.getEndpoints(), options.getRouting(), options.getRoutePolicies());
        RuntimeException last = null;
        for (ProductEndpointConfig endpoint : candidates) {
            if (options.isCircuitBreakerEnabled() && !breaker(endpoint).allowRequest()) {
                continue;
            }
            ProductModelProvider provider = providers.get(endpoint.getProvider());
            if (provider == null) {
                last = new IllegalStateException("provider not found: " + endpoint.getProvider());
                continue;
            }
            try {
                AgentChatResponse response = agentEngine.chat(request, endpoint, provider, logicalModel);
                breaker(endpoint).onSuccess();
                return response;
            } catch (RuntimeException ex) {
                breaker(endpoint).onFailure(ex.getMessage());
                last = ex;
            }
        }
        throw new IllegalStateException("all embedded endpoints failed: "
                + (last == null ? "no route found" : last.getMessage()), last);
    }

    private EndpointCircuitBreaker breaker(ProductEndpointConfig endpoint) {
        return circuitBreakers.computeIfAbsent(endpoint.getId(),
                k -> new EndpointCircuitBreaker(options.getCircuitFailureThreshold(), options.getCircuitOpenSeconds()));
    }

    @Override
    public Map<String, EndpointCircuitBreaker.Snapshot> circuitBreakerSnapshot() {
        Map<String, EndpointCircuitBreaker.Snapshot> out = new ConcurrentHashMap<>();
        circuitBreakers.forEach((k, v) -> out.put(k, v.snapshot()));
        return out;
    }
}
