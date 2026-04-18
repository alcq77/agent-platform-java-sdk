package io.github.alcq77.cqgent.product.core.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqgent.product.core.session.ProductSessionChatMemory;
import io.github.alcq77.cqgent.product.core.tool.ProductToolRegistry;
import io.github.alcq77.cqgent.product.spi.session.ProductSessionStore;
import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 基于 LangChain4j 抽象的 cqgent 运行时门面。
 */
public class LangChain4jProductAgentRuntime {

    /**
     * 会话历史后端（内存/文件/redis/jdbc）。
     */
    private final ProductSessionStore sessionStore;
    /**
     * ProductTool -> ToolSpecification / ToolExecution 适配层。
     */
    private final ProductToolRegistry toolRegistry;
    /**
     * 工具回路最大轮次，防止模型与工具陷入无限循环。
     */
    private final int maxToolCallIterations;
    private final Map<String, PromptTemplate> promptTemplates;
    private final String defaultPromptTemplateId;
    private final boolean fallbackToDefaultPromptTemplate;
    private final Map<String, LongAdder> templateHits = new ConcurrentHashMap<>();
    private final LongAdder templateFallbackCount = new LongAdder();
    private final LongAdder templateMissingCount = new LongAdder();

    public LangChain4jProductAgentRuntime(ProductSessionStore sessionStore,
                                          List<ProductTool> tools,
                                          int maxToolCallIterations,
                                          Map<String, PromptTemplate> promptTemplates,
                                          String defaultPromptTemplateId,
                                          boolean fallbackToDefaultPromptTemplate) {
        this.sessionStore = sessionStore;
        this.toolRegistry = new ProductToolRegistry(tools);
        this.maxToolCallIterations = Math.max(1, maxToolCallIterations);
        this.promptTemplates = promptTemplates == null ? Map.of() : promptTemplates;
        this.defaultPromptTemplateId = defaultPromptTemplateId;
        this.fallbackToDefaultPromptTemplate = fallbackToDefaultPromptTemplate;
    }

    public AgentChatResponse chat(AgentChatRequest request, ChatLanguageModel model, String logicalModel) {
        String sessionId = resolveSessionId(request.getSessionId());
        PromptTemplate selectedTemplate = resolveTemplate(request.getPromptTemplateId());
        String systemPromptSource = firstNonBlank(request.getSystemPrompt(),
                selectedTemplate == null ? null : selectedTemplate.systemPrompt());
        String userPromptSource = firstNonBlank(request.getMessage(),
                selectedTemplate == null ? null : selectedTemplate.userMessage());
        String renderedSystemPrompt = renderTemplate(systemPromptSource, request.getPromptVariables());
        String renderedMessage = renderTemplate(userPromptSource, request.getPromptVariables());
        if (renderedMessage == null || renderedMessage.isBlank()) {
            throw new IllegalStateException("rendered user message is empty");
        }

        // 读取历史并进入本次会话 memory
        ProductSessionChatMemory memory = new ProductSessionChatMemory(sessionId, sessionStore);
        if (renderedSystemPrompt != null && !renderedSystemPrompt.isBlank()) {
            memory.add(SystemMessage.from(renderedSystemPrompt.trim()));
        }
        UserMessage currentUser = UserMessage.from(renderedMessage);
        memory.add(currentUser);

        TokenUsage totalUsage = new TokenUsage();
        AiMessage finalAiMessage = null;
        for (int i = 0; i < maxToolCallIterations; i++) {
            // 由 LangChain4j 驱动模型调用与工具规范传递
            Response<AiMessage> response = toolRegistry.isEmpty()
                    ? model.generate(memory.messages())
                    : model.generate(memory.messages(), toolRegistry.specifications());
            if (response.tokenUsage() != null) {
                totalUsage = totalUsage.add(response.tokenUsage());
            }
            AiMessage aiMessage = response.content();
            memory.add(aiMessage);
            if (!aiMessage.hasToolExecutionRequests()) {
                finalAiMessage = aiMessage;
                break;
            }
            for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                String toolResult = toolRegistry.execute(toolRequest);
                memory.add(ToolExecutionResultMessage.from(toolRequest, toolResult));
            }
        }
        if (finalAiMessage == null) {
            throw new IllegalStateException("tool loop exceeded max iterations: " + maxToolCallIterations);
        }
        // 请求结束后一次性持久化会话消息
        memory.syncToStore();
        return AgentChatResponse.builder()
                .sessionId(sessionId)
                .reply(finalAiMessage.text())
                .inputTokens(totalUsage.inputTokenCount())
                .outputTokens(totalUsage.outputTokenCount())
                .totalTokens(totalUsage.totalTokenCount())
                .traceId(request.getTraceId())
                .build();
    }

    private String resolveSessionId(String requested) {
        if (requested != null && !requested.isBlank()) {
            String sid = requested.trim();
            if (!sessionStore.hasSession(sid)) {
                sessionStore.register(sid);
            }
            return sid;
        }
        String sid = UUID.randomUUID().toString();
        sessionStore.register(sid);
        return sid;
    }

    private static String renderTemplate(String template, Map<String, String> variables) {
        if (template == null || template.isBlank() || variables == null || variables.isEmpty()) {
            return template;
        }
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            rendered = rendered.replace("{{" + key + "}}", value);
        }
        return rendered;
    }

    private PromptTemplate resolveTemplate(String requestTemplateId) {
        if (requestTemplateId == null || requestTemplateId.isBlank()) {
            PromptTemplate template = resolveDefaultTemplateOrNull();
            if (template != null) {
                markTemplateHit(defaultPromptTemplateId, false);
            }
            return template;
        }
        String requestId = requestTemplateId.trim();
        PromptTemplate template = promptTemplates.get(requestId);
        if (template != null) {
            markTemplateHit(requestId, false);
            return template;
        }
        templateMissingCount.increment();
        if (fallbackToDefaultPromptTemplate) {
            PromptTemplate fallback = resolveDefaultTemplateOrNull();
            if (fallback != null) {
                markTemplateHit(defaultPromptTemplateId, true);
            }
            return fallback;
        }
        throw new IllegalStateException("prompt template not found: " + requestId);
    }

    private PromptTemplate resolveDefaultTemplateOrNull() {
        if (defaultPromptTemplateId == null || defaultPromptTemplateId.isBlank()) {
            return null;
        }
        PromptTemplate template = promptTemplates.get(defaultPromptTemplateId.trim());
        if (template == null) {
            throw new IllegalStateException("default prompt template not found: " + defaultPromptTemplateId);
        }
        return template;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    public Map<String, Object> promptTemplateMetrics() {
        Map<String, Long> hitByTemplate = new LinkedHashMap<>();
        templateHits.forEach((k, v) -> hitByTemplate.put(k, v.longValue()));
        return Map.of(
                "templateHits", hitByTemplate,
                "templateFallbackCount", templateFallbackCount.longValue(),
                "templateMissingCount", templateMissingCount.longValue()
        );
    }

    private void markTemplateHit(String templateId, boolean fallback) {
        if (templateId != null && !templateId.isBlank()) {
            templateHits.computeIfAbsent(templateId.trim(), k -> new LongAdder()).increment();
        }
        if (fallback) {
            templateFallbackCount.increment();
        }
    }

    public record PromptTemplate(String systemPrompt, String userMessage) {
    }
}
