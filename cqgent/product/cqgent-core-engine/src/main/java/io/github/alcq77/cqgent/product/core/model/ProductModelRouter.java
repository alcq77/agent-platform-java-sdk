package io.github.alcq77.cqgent.product.core.model;

import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内嵌版模型路由器：支持静态路由 + 主备 + 加权 + 健康感知。
 */
public class ProductModelRouter {

    public interface EndpointHealthChecker {
        boolean reachable(ProductEndpointConfig endpoint);
    }

    private final EndpointHealthChecker healthChecker;
    private final Map<String, AtomicInteger> cursor = new ConcurrentHashMap<>();

    public ProductModelRouter(EndpointHealthChecker healthChecker) {
        this.healthChecker = healthChecker;
    }

    public List<ProductEndpointConfig> resolveCandidates(String logicalModel,
                                                         Map<String, ProductEndpointConfig> endpointById,
                                                         Map<String, String> routing,
                                                         Map<String, RoutePolicy> policies) {
        List<String> endpointIds = new ArrayList<>();
        RoutePolicy policy = policies.get(logicalModel);
        if (policy != null) {
            endpointIds.addAll(policyOrder(logicalModel, policy));
        }
        String staticRoute = routing.get(logicalModel);
        if (staticRoute != null && !staticRoute.isBlank()) {
            endpointIds.add(staticRoute);
        }
        if (endpointIds.isEmpty() && endpointById.containsKey(logicalModel)) {
            endpointIds.add(logicalModel);
        }
        List<ProductEndpointConfig> out = new ArrayList<>();
        boolean healthAware = policy == null || policy.isHealthAware();
        for (String id : new LinkedHashSet<>(endpointIds)) {
            ProductEndpointConfig endpoint = endpointById.get(id);
            if (endpoint == null) {
                continue;
            }
            if (!healthAware || healthChecker.reachable(endpoint)) {
                out.add(endpoint);
            }
        }
        return out;
    }

    private List<String> policyOrder(String logicalModel, RoutePolicy policy) {
        List<String> out = new ArrayList<>();
        if (policy.getPrimaryEndpoint() != null && !policy.getPrimaryEndpoint().isBlank()) {
            out.add(policy.getPrimaryEndpoint().trim());
        }
        if (policy.getWeightedEndpoints() != null && !policy.getWeightedEndpoints().isEmpty()) {
            List<String> weighted = new ArrayList<>();
            for (Map.Entry<String, Integer> e : policy.getWeightedEndpoints().entrySet()) {
                int weight = Math.max(0, e.getValue() == null ? 0 : e.getValue());
                for (int i = 0; i < weight; i++) {
                    weighted.add(e.getKey());
                }
            }
            if (!weighted.isEmpty()) {
                int idx = Math.floorMod(cursor.computeIfAbsent(logicalModel, k -> new AtomicInteger(0))
                        .getAndIncrement(), weighted.size());
                String chosen = weighted.get(idx);
                out.add(chosen);
                for (String v : weighted) {
                    if (!v.equals(chosen)) {
                        out.add(v);
                    }
                }
            }
        }
        out.addAll(policy.getSecondaryEndpoints());
        return out;
    }
}
