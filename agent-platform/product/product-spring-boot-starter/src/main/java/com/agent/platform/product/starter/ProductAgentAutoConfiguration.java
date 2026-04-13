package com.agent.platform.product.starter;

import com.agent.platform.product.sdk.AgentClient;
import com.agent.platform.product.sdk.AgentClientBuilder;
import com.agent.platform.product.sdk.internal.CircuitBreakerSnapshotProvider;
import com.agent.platform.product.spi.model.ProductEndpointConfig;
import com.agent.platform.product.spi.model.ProductModelProvider;
import com.agent.platform.product.spi.tool.ProductTool;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@AutoConfiguration
@ConditionalOnClass(AgentClient.class)
@EnableConfigurationProperties(ProductStarterProperties.class)
public class ProductAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentClient productAgentClient(ProductStarterProperties properties,
                                          ObjectProvider<ProductModelProvider> modelProviders,
                                          ObjectProvider<ProductTool> tools) {
        AgentClientBuilder builder = AgentClientBuilder.create()
                .logicalModel(properties.getLogicalModel())
                .maxHistoryMessages(properties.getMaxHistoryMessages());
        properties.getRouting().forEach(builder::route);
        properties.getRoutePolicies().forEach(builder::routePolicy);
        properties.getEndpoints().forEach((id, ep) -> builder.endpoint(ProductEndpointConfig.builder()
                .id(id)
                .provider(ep.getProvider())
                .baseUrl(ep.getBaseUrl())
                .apiKey(ep.getApiKey())
                .defaultModel(ep.getDefaultModel())
                .headers(ep.getHeaders())
                .connectTimeout(ep.getConnectTimeout())
                .readTimeout(ep.getReadTimeout())
                .build()));
        modelProviders.orderedStream().forEach(builder::modelProvider);
        tools.orderedStream().forEach(builder::tool);
        return builder.build();
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(ProductStarterSanityChecker.class)
    public ProductStarterSanityChecker productStarterSanityChecker(ProductStarterProperties properties) {
        return new ProductStarterSanityChecker(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ProductWorkspaceInitializer.class)
    public ProductWorkspaceInitializer productWorkspaceInitializer(ProductStarterProperties properties) {
        return new ProductWorkspaceInitializer(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.plugin", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ProductPluginDirectoryToolRegistry productPluginDirectoryToolRegistry(ProductStarterProperties properties) {
        return new ProductPluginDirectoryToolRegistry(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.plugin", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ProductTool productPluginDispatcherTool(ProductPluginDirectoryToolRegistry registry) {
        return new ProductPluginDispatcherTool(registry);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public WorkspaceSkillsRegistry workspaceSkillsRegistry(ProductStarterProperties properties) {
        return new WorkspaceSkillsRegistry(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.product.skills", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(name = "workspaceSkillsTool")
    public ProductTool workspaceSkillsTool(WorkspaceSkillsRegistry registry) {
        return new WorkspaceSkillsTool(registry);
    }

    @Bean
    @ConditionalOnMissingBean(name = "productStarterHealthIndicator")
    public HealthIndicator productStarterHealthIndicator(ProductStarterProperties properties, AgentClient agentClient) {
        return () -> {
            if (properties.getEndpoints().isEmpty()) {
                return Health.down().withDetail("reason", "agent.product.endpoints is empty").build();
            }
            Health.Builder health = Health.up()
                    .withDetail("logicalModel", properties.getLogicalModel())
                    .withDetail("workspace", properties.getWorkspace());
            if (agentClient instanceof CircuitBreakerSnapshotProvider provider) {
                health.withDetail("circuitBreakers", provider.circuitBreakerSnapshot());
            }
            return health.build();
        };
    }
}
