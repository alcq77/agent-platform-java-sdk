package io.github.alcq77.cqagent.product.starter;

import io.github.alcq77.cqagent.product.core.model.ModelDispatchPolicy;
import io.github.alcq77.cqagent.product.core.model.RoutePolicy;
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

    /**
     * 单次调用超时时间（毫秒），0 表示不限制。
     */
    private long invokeTimeoutMs = 60_000;

    /**
     * 单端点失败后的最大重试次数（不含首次调用）。
     */
    private int maxRetries = 1;

    /**
     * 重试退避时长（毫秒）。
     */
    private long retryBackoffMs = 300;

    private Map<String, Endpoint> endpoints = new LinkedHashMap<>();

    private Map<String, String> routing = new LinkedHashMap<>();

    private Map<String, RoutePolicy> routePolicies = new LinkedHashMap<>();

    private Map<String, ModelDispatchPolicy> modelDispatchPolicies = new LinkedHashMap<>();

    private Plugin plugin = new Plugin();

    private Skills skills = new Skills();

    private Session session = new Session();

    private Prompts prompts = new Prompts();

    private Rag rag = new Rag();

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

    @Data
    public static class Session {
        /**
         * 会话存储类型：in_memory / filesystem / redis / jdbc。
         */
        private String store = "in_memory";

        /**
         * Redis key 前缀，仅在 store=redis 时生效。
         */
        private String redisKeyPrefix = "cqgent:session:";

        /**
         * Redis 会话 TTL，默认 7 天；每次写入会刷新 TTL。
         */
        private Duration redisTtl = Duration.ofDays(7);

        /**
         * JDBC 表名，仅在 store=jdbc 时生效。
         */
        private String jdbcTable = "cqgent_session_store";

        /**
         * 是否在启动时自动建表（执行 create table if not exists）。
         */
        private boolean jdbcAutoInit = true;

        /**
         * 文件会话存储目录（store=filesystem 时生效）。
         */
        private String filesystemDirectory = "./workspace/sessions";
    }

    @Data
    public static class Prompts {
        /**
         * 默认模板 ID。
         */
        private String defaultTemplateId;

        /**
         * 模板缺失时是否回退到默认模板。
         */
        private boolean fallbackToDefault = true;

        /**
         * 命名模板集合（key=templateId）。
         */
        private Map<String, PromptTemplate> templates = new LinkedHashMap<>();
    }

    @Data
    public static class PromptTemplate {
        private String version;
        private String systemPrompt;
        private String userMessage;
    }

    @Data
    public static class Rag {
        /**
         * 是否启用 RAG 增强 advisor。
         */
        private boolean enabled = false;
        /**
         * 本地知识库目录。
         */
        private String knowledgeDirectory = "./workspace/knowledge";
        /**
         * 索引清单文件路径。
         */
        private String manifestPath = "./workspace/rag/index-manifest.json";
        /**
         * 切分参数。
         */
        private int chunkSize = 400;
        private int overlap = 80;
        /**
         * 检索参数。
         */
        private int topK = 4;
        private int advisorOrder = -100;
        /**
         * 过滤条件：source 字符串包含匹配。
         */
        private java.util.List<String> allowedSources = new java.util.ArrayList<>();
        /**
         * 过滤条件：metadata 等值匹配。
         */
        private java.util.Map<String, String> metadataEquals = new java.util.LinkedHashMap<>();
    }
}
