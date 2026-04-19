package io.github.alcq77.cqgent.product.sdk.internal;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqgent.product.core.agent.LangChain4jProductAgentRuntime;
import io.github.alcq77.cqgent.product.core.model.ProductModelRouter;
import io.github.alcq77.cqgent.product.core.tool.ProductToolRegistry;
import io.github.alcq77.cqgent.product.sdk.AgentClient;
import io.github.alcq77.cqgent.product.sdk.AgentStreamingListener;
import io.github.alcq77.cqgent.product.sdk.ProductSdkOptions;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqgent.product.spi.model.ProductModelProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class EmbeddedAgentClient implements AgentClient, CircuitBreakerSnapshotProvider, AgentRuntimeMetricsProvider {

    private final ProductSdkOptions options;
    /**
     * 只负责 endpoint 选路，不负责模型调用细节。
     */
    private final ProductModelRouter modelRouter;
    /**
     * LangChain4j 基座运行时门面。
     */
    private final LangChain4jProductAgentRuntime runtime;
    private final Map<String, ProductModelProvider> providers;
    private final Map<String, EndpointCircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalFailures = new LongAdder();
    private final Map<String, LongAdder> endpointFailures = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failureByType = new ConcurrentHashMap<>();
    private final LongAdder circuitSkipped = new LongAdder();

    public EmbeddedAgentClient(ProductSdkOptions options,
                               ProductModelRouter modelRouter,
                               LangChain4jProductAgentRuntime runtime,
                               Map<String, ProductModelProvider> providers) {
        this.options = options;
        this.modelRouter = modelRouter;
        this.runtime = runtime;
        this.providers = providers;
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        totalRequests.increment();
        String traceId = resolveTraceId(request);
        String logicalModel = options.getLogicalModel();
        List<ProductEndpointConfig> candidates = modelRouter.resolveCandidates(
                logicalModel, options.getEndpoints(), options.getRouting(), options.getRoutePolicies());
        if (candidates.isEmpty()) {
            markFailure("route", "none", new IllegalStateException("no route found"));
        }
        RuntimeException last = null;
        for (ProductEndpointConfig endpoint : candidates) {
            // 熔断只作用于 endpoint 维度，避免单点持续雪崩。
            if (options.isCircuitBreakerEnabled() && !breaker(endpoint).allowRequest()) {
                circuitSkipped.increment();
                log.warn("traceId={} endpoint={} skipped by circuit breaker", traceId, endpoint.getId());
                continue;
            }
            ProductModelProvider provider = providers.get(endpoint.getProvider());
            if (provider == null) {
                last = new IllegalStateException("provider not found: " + endpoint.getProvider());
                markFailure("provider", endpoint.getId(), last);
                continue;
            }
            for (int attempt = 0; attempt <= options.getMaxRetries(); attempt++) {
                try {
                    request.setTraceId(traceId);
                    AgentChatResponse response = invokeWithTimeout(request, endpoint, provider, logicalModel);
                    breaker(endpoint).onSuccess();
                    response.setTraceId(traceId);
                    return response;
                } catch (RuntimeException ex) {
                    breaker(endpoint).onFailure(ex.getMessage());
                    markFailure(classifyFailureType(ex), endpoint.getId(), ex);
                    last = ex;
                    if (attempt < options.getMaxRetries()) {
                        sleepBackoff();
                    }
                }
            }
        }
        throw new IllegalStateException("traceId=" + traceId + ", all embedded endpoints failed: "
                + (last == null ? "no route found" : last.getMessage()), last);
    }

    @Override
    public void stream(AgentChatRequest request, AgentStreamingListener listener) {
        totalRequests.increment();
        runtime.runtimeCounters().incrementStreamingInvocation();
        String traceId = resolveTraceId(request);
        String logicalModel = options.getLogicalModel();
        List<ProductEndpointConfig> candidates = modelRouter.resolveCandidates(
            logicalModel, options.getEndpoints(), options.getRouting(), options.getRoutePolicies());
        if (candidates.isEmpty()) {
            RuntimeException ex = new IllegalStateException("no route found");
            markFailure("route", "none", ex);
            listener.onError(ex);
            return;
        }
        RuntimeException last = null;
        for (ProductEndpointConfig endpoint : candidates) {
            if (options.isCircuitBreakerEnabled() && !breaker(endpoint).allowRequest()) {
                circuitSkipped.increment();
                continue;
            }
            ProductModelProvider provider = providers.get(endpoint.getProvider());
            if (provider == null) {
                last = new IllegalStateException("provider not found: " + endpoint.getProvider());
                markFailure("provider", endpoint.getId(), last);
                continue;
            }
            for (int attempt = 0; attempt <= options.getMaxRetries(); attempt++) {
                try {
                    request.setTraceId(traceId);
                    invokeStreamingRuntime(request, endpoint, provider, logicalModel, listener);
                    breaker(endpoint).onSuccess();
                    return;
                } catch (RuntimeException ex) {
                    breaker(endpoint).onFailure(ex.getMessage());
                    markFailure(classifyFailureType(ex), endpoint.getId(), ex);
                    last = ex;
                    if (attempt < options.getMaxRetries()) {
                        sleepBackoff();
                    }
                }
            }
        }
        listener.onError(new IllegalStateException("traceId=" + traceId + ", all embedded endpoints failed: "
            + (last == null ? "no route found" : last.getMessage()), last));
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

    @Override
    public Map<String, Object> runtimeMetrics() {
        Map<String, Long> failureByEndpoint = new ConcurrentHashMap<>();
        endpointFailures.forEach((k, v) -> failureByEndpoint.put(k, v.longValue()));
        Map<String, Long> typedFailures = new ConcurrentHashMap<>();
        failureByType.forEach((k, v) -> typedFailures.put(k, v.longValue()));
        return Map.of(
                "totalRequests", totalRequests.longValue(),
                "totalFailures", totalFailures.longValue(),
                "endpointFailures", failureByEndpoint,
                "failureByType", typedFailures,
                "circuitSkipped", circuitSkipped.longValue(),
                "promptTemplates", runtime.promptTemplateMetrics()
        );
    }

    private static String resolveTraceId(AgentChatRequest request) {
        if (request.getTraceId() != null && !request.getTraceId().isBlank()) {
            return request.getTraceId().trim();
        }
        return UUID.randomUUID().toString();
    }

    private void markFailure(String type, String endpointId, RuntimeException ex) {
        totalFailures.increment();
        endpointFailures.computeIfAbsent(endpointId, k -> new LongAdder()).increment();
        failureByType.computeIfAbsent(type, k -> new LongAdder()).increment();
        log.warn("endpoint invoke failed, type={}, endpointId={}, message={}", type, endpointId, ex.getMessage());
    }

    private static String classifyFailureType(RuntimeException ex) {
        String raw = ex.getMessage() == null ? "" : ex.getMessage();
        if (raw.startsWith(ProductToolRegistry.PREFIX_VALIDATION)) {
            return "tool_validation";
        }
        if (raw.startsWith(ProductToolRegistry.PREFIX_EXECUTION)) {
            return "tool_execution";
        }
        String message = raw.toLowerCase();
        if (message.contains("timeout")) {
            return "timeout";
        }
        if (message.contains("tool")) {
            return "tool";
        }
        if (message.contains("route")) {
            return "route";
        }
        return "upstream";
    }

    private AgentChatResponse invokeWithTimeout(AgentChatRequest request,
                                                ProductEndpointConfig endpoint,
                                                ProductModelProvider provider,
                                                String logicalModel) {
        long timeoutMs = options.getInvokeTimeoutMs();
        if (timeoutMs <= 0) {
            return invokeRuntime(request, endpoint, provider, logicalModel);
        }
        CompletableFuture<AgentChatResponse> future = CompletableFuture.supplyAsync(
                () -> invokeRuntime(request, endpoint, provider, logicalModel)
        );
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new IllegalStateException("invoke timeout after " + timeoutMs + "ms", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("invoke interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("invoke execution failed", cause);
        }
    }

    private AgentChatResponse invokeRuntime(AgentChatRequest request,
                                            ProductEndpointConfig endpoint,
                                            ProductModelProvider provider,
                                            String logicalModel) {
        // 由 provider 生成 LangChain4j 模型，再交给 runtime 执行统一对话流程。
        ChatLanguageModel model = provider.createChatLanguageModel(endpoint, logicalModel);
        return runtime.chat(request, model, logicalModel);
    }

    private void invokeStreamingRuntime(AgentChatRequest request,
                                        ProductEndpointConfig endpoint,
                                        ProductModelProvider provider,
                                        String logicalModel,
                                        AgentStreamingListener listener) {
        StreamingChatLanguageModel model = provider.createStreamingChatLanguageModel(endpoint, logicalModel);
        runtime.stream(
            request,
            model,
            logicalModel,
            listener::onToken,
            response -> {
                response.setTraceId(request.getTraceId());
                listener.onComplete(response);
            },
            listener::onError
        );
    }

    private void sleepBackoff() {
        if (options.getRetryBackoffMs() <= 0) {
            return;
        }
        try {
            Thread.sleep(options.getRetryBackoffMs());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
