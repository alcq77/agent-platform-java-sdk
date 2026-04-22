package io.github.alcq77.cqagent.product.starter;

import io.github.alcq77.cqagent.product.spi.session.ProductSessionStore;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话存储观测端点：
 * /actuator/cqgentSessionStore
 */
@Endpoint(id = "cqgentSessionStore")
public class ProductSessionStoreEndpoint {

    private final ProductStarterProperties properties;
    private final ProductSessionStore sessionStore;

    public ProductSessionStoreEndpoint(ProductStarterProperties properties, ProductSessionStore sessionStore) {
        this.properties = properties;
        this.sessionStore = sessionStore;
    }

    @ReadOperation
    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("configuredStore", properties.getSession().getStore());
        if (sessionStore == null) {
            out.put("sessionStoreBean", "none");
            out.put("healthy", false);
            out.put("detail", "ProductSessionStore bean not found");
            return out;
        }
        out.put("sessionStoreBean", sessionStore.getClass().getName());
        if (sessionStore instanceof SessionStoreHealthProbe probe) {
            out.put("healthy", probe.healthy());
            out.put("detail", probe.detail());
        } else {
            out.put("healthy", true);
            out.put("detail", "no probe");
        }
        if (sessionStore instanceof SessionStoreMetricsProvider metricsProvider) {
            out.put("metrics", metricsProvider.metrics());
        }
        return out;
    }
}
