package io.github.alcq77.cqgent.product.starter;

import io.github.alcq77.cqgent.product.core.model.RoutePolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "agent.product")
public class ProductStarterProperties {

    /**
     * 本地工作目录，存放技能、上下文与运行期文件。
     */
    private String workspace = "./workspace";

    private String logicalModel = "primary-llm";

    private int maxHistoryMessages = 40;

    private Map<String, Endpoint> endpoints = new LinkedHashMap<>();

    private Map<String, String> routing = new LinkedHashMap<>();

    private Map<String, RoutePolicy> routePolicies = new LinkedHashMap<>();

    private Plugin plugin = new Plugin();

    private Skills skills = new Skills();

    @Data
    public static class Endpoint {
        private String provider = "openai_compat";
        private String baseUrl;
        private String apiKey;
        private String defaultModel;
        private Map<String, String> headers = new LinkedHashMap<>();
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(120);
    }

    @Data
    public static class Plugin {
        private boolean enabled = true;
        private String directory = "./workspace/plugins";
        private long reloadIntervalSeconds = 5;
    }

    @Data
    public static class Skills {
        private boolean enabled = true;
        private String directory = "./workspace/skills";
    }
}
