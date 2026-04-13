package com.agent.platform.product.starter;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * 启动期配置自检，优先给出可读错误，减少运行期排障成本。
 */
public class ProductStarterSanityChecker implements ApplicationRunner {

    private final ProductStarterProperties properties;

    public ProductStarterSanityChecker(ProductStarterProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getLogicalModel() == null || properties.getLogicalModel().isBlank()) {
            throw new IllegalStateException("agent.product.logical-model must not be blank");
        }
        if (properties.getEndpoints().isEmpty()) {
            throw new IllegalStateException("agent.product.endpoints must not be empty");
        }
        String logicalModel = properties.getLogicalModel();
        boolean routed = properties.getRouting().containsKey(logicalModel)
                || properties.getRoutePolicies().containsKey(logicalModel)
                || properties.getEndpoints().containsKey(logicalModel);
        if (!routed) {
            throw new IllegalStateException(
                    "logical model '" + logicalModel + "' is not routable, please configure routing/route-policies/endpoints");
        }
    }
}
