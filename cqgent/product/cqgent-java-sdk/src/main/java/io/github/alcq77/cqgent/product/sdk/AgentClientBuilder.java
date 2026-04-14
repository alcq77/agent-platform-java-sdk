package io.github.alcq77.cqgent.product.sdk;

import io.github.alcq77.cqgent.product.core.agent.ProductAgentEngine;
import io.github.alcq77.cqgent.product.core.model.ProductModelRouter;
import io.github.alcq77.cqgent.product.core.model.RoutePolicy;
import io.github.alcq77.cqgent.product.core.session.InMemoryProductSessionStore;
import io.github.alcq77.cqgent.product.sdk.internal.EmbeddedAgentClient;
import io.github.alcq77.cqgent.product.sdk.provider.OpenAiCompatibleProductProvider;
import io.github.alcq77.cqgent.product.sdk.tool.CurrentTimeProductTool;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqgent.product.spi.model.ProductModelProvider;
import io.github.alcq77.cqgent.product.spi.session.ProductSessionStore;
import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class AgentClientBuilder {

    private final ProductSdkOptions options = new ProductSdkOptions();
    private final List<ProductTool> tools = new ArrayList<>();
    private final Map<String, ProductModelProvider> providers = new LinkedHashMap<>();
    private ProductSessionStore sessionStore;
    private ProductModelRouter.EndpointHealthChecker healthChecker = endpoint -> true;

    public static AgentClientBuilder create() {
        return new AgentClientBuilder();
    }

    public AgentClientBuilder logicalModel(String logicalModel) {
        options.setLogicalModel(logicalModel);
        return this;
    }

    public AgentClientBuilder maxHistoryMessages(int maxHistoryMessages) {
        options.setMaxHistoryMessages(maxHistoryMessages);
        return this;
    }

    public AgentClientBuilder endpoint(ProductEndpointConfig endpoint) {
        options.getEndpoints().put(endpoint.getId(), endpoint);
        return this;
    }

    public AgentClientBuilder route(String logicalModel, String endpointId) {
        options.getRouting().put(logicalModel, endpointId);
        return this;
    }

    public AgentClientBuilder routePolicy(String logicalModel, RoutePolicy policy) {
        options.getRoutePolicies().put(logicalModel, policy);
        return this;
    }

    public AgentClientBuilder maxToolCallIterations(int maxIterations) {
        options.setMaxToolCallIterations(Math.max(1, maxIterations));
        return this;
    }

    public AgentClientBuilder circuitBreaker(boolean enabled, int failureThreshold, int openSeconds) {
        options.setCircuitBreakerEnabled(enabled);
        options.setCircuitFailureThreshold(Math.max(1, failureThreshold));
        options.setCircuitOpenSeconds(Math.max(1, openSeconds));
        return this;
    }

    public AgentClientBuilder invokeTimeoutMillis(long timeoutMs) {
        options.setInvokeTimeoutMs(Math.max(0, timeoutMs));
        return this;
    }

    public AgentClientBuilder retry(int maxRetries, long backoffMs) {
        options.setMaxRetries(Math.max(0, maxRetries));
        options.setRetryBackoffMs(Math.max(0, backoffMs));
        return this;
    }

    public AgentClientBuilder sessionStore(ProductSessionStore store) {
        this.sessionStore = store;
        return this;
    }

    public AgentClientBuilder healthChecker(ProductModelRouter.EndpointHealthChecker checker) {
        this.healthChecker = checker;
        return this;
    }

    public AgentClientBuilder tool(ProductTool tool) {
        this.tools.add(tool);
        return this;
    }

    public AgentClientBuilder modelProvider(ProductModelProvider provider) {
        this.providers.put(provider.providerCode(), provider);
        return this;
    }

    public AgentClient build() {
        ProductSessionStore store = sessionStore != null
                ? sessionStore
                : new InMemoryProductSessionStore(options.getMaxHistoryMessages());
        ServiceLoader.load(ProductTool.class).forEach(this::tool);
        ServiceLoader.load(ProductModelProvider.class).forEach(this::modelProvider);
        if (tools.isEmpty()) {
            tools.add(new CurrentTimeProductTool());
        }
        if (providers.isEmpty()) {
            ProductModelProvider openai = new OpenAiCompatibleProductProvider();
            providers.put(openai.providerCode(), openai);
        }
        ProductModelRouter router = new ProductModelRouter(healthChecker);
        ProductAgentEngine agentEngine = new ProductAgentEngine(store, tools, options.getMaxToolCallIterations());
        return new EmbeddedAgentClient(options, router, agentEngine, providers);
    }
}
