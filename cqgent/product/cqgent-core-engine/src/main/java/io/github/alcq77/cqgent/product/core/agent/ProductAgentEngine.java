package io.github.alcq77.cqgent.product.core.agent;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatResponse;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionChoiceDto;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionRequest;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionResponse;
import io.github.alcq77.cqgent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqgent.model.api.dto.TokenUsageDto;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqgent.product.spi.model.ProductModelProvider;
import io.github.alcq77.cqgent.product.spi.session.ProductSessionStore;
import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 与 Spring 无关的内嵌 Agent 引擎。
 */
public class ProductAgentEngine {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final ProductSessionStore sessionStore;
    private final List<ProductTool> tools;
    private final int maxToolCallIterations;

    public ProductAgentEngine(ProductSessionStore sessionStore, List<ProductTool> tools, int maxToolCallIterations) {
        this.sessionStore = sessionStore;
        this.tools = tools;
        this.maxToolCallIterations = Math.max(1, maxToolCallIterations);
    }

    public AgentChatResponse chat(AgentChatRequest request,
                                  ProductEndpointConfig endpoint,
                                  ProductModelProvider provider,
                                  String logicalModel) {
        String sessionId = resolveSessionId(request.getSessionId());
        List<ChatMessageDto> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(ChatMessageDto.builder().role(ROLE_SYSTEM).content(request.getSystemPrompt().trim()).build());
        }
        appendToolGuide(messages);
        messages.addAll(sessionStore.history(sessionId));
        ChatMessageDto currentUser = ChatMessageDto.builder().role(ROLE_USER).content(request.getMessage()).build();
        messages.add(currentUser);

        ModelRound round = invokeWithToolLoop(endpoint, provider, logicalModel, messages);
        String answer = round.answer();
        ChatMessageDto assistant = ChatMessageDto.builder().role(ROLE_ASSISTANT).content(answer).build();
        sessionStore.append(sessionId, currentUser, assistant);

        TokenUsageDto usage = round.usage();
        return AgentChatResponse.builder()
                .sessionId(sessionId)
                .reply(answer)
                .inputTokens(usage == null ? null : usage.getPromptTokens())
                .outputTokens(usage == null ? null : usage.getCompletionTokens())
                .totalTokens(usage == null ? null : usage.getTotalTokens())
                .build();
    }

    /**
     * 在多轮调用中执行标准 tool-calling：
     * 1) 模型返回工具调用指令
     * 2) 执行工具并把结果追加回消息上下文
     * 3) 再次请求模型，直到得到最终回答或达到最大回合
     */
    private ModelRound invokeWithToolLoop(ProductEndpointConfig endpoint,
                                          ProductModelProvider provider,
                                          String logicalModel,
                                          List<ChatMessageDto> messages) {
        Map<String, ProductTool> toolMap = toolMap();
        TokenUsageDto lastUsage = null;
        String answer = "";
        for (int i = 0; i < maxToolCallIterations; i++) {
            ChatCompletionRequest modelReq = ChatCompletionRequest.builder()
                    .model(logicalModel)
                    .messages(messages)
                    .build();
            ChatCompletionResponse completion = provider.complete(endpoint, modelReq);
            answer = firstText(completion);
            lastUsage = completion.getUsage();
            ToolCall toolCall = parseToolCall(answer);
            if (toolCall == null) {
                return new ModelRound(answer, lastUsage);
            }
            if (!isSafeToolName(toolCall.toolName())) {
                throw new IllegalStateException("tool name contains illegal characters: " + toolCall.toolName());
            }
            if (!isSafeToolInput(toolCall.input())) {
                throw new IllegalStateException("tool input exceeds safety limit");
            }
            ProductTool tool = toolMap.get(toolCall.toolName());
            if (tool == null) {
                return new ModelRound(answer, lastUsage);
            }
            String toolResult = tool.execute(toolCall.input());
            messages.add(ChatMessageDto.builder().role(ROLE_ASSISTANT).content(answer).build());
            messages.add(ChatMessageDto.builder()
                    .role(ROLE_SYSTEM)
                    .content("ToolResult(" + toolCall.toolName() + "): " + toolResult)
                    .build());
        }
        return new ModelRound(answer, lastUsage);
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

    /**
     * 将可用工具声明加入系统上下文，指导模型使用统一调用格式。
     */
    private void appendToolGuide(List<ChatMessageDto> messages) {
        if (tools.isEmpty()) {
            return;
        }
        String names = tools.stream().map(ProductTool::name).distinct().reduce((a, b) -> a + "," + b).orElse("");
        messages.add(ChatMessageDto.builder()
                .role(ROLE_SYSTEM)
                .content("When tool is needed, output exactly: tool_call:<toolName>|<input>. Available tools: " + names)
                .build());
    }

    private static String firstText(ChatCompletionResponse response) {
        List<ChatCompletionChoiceDto> choices = response.getChoices();
        if (choices == null || choices.isEmpty() || choices.get(0).getMessage() == null) {
            throw new IllegalStateException("model returned empty assistant message");
        }
        return choices.get(0).getMessage().getContent();
    }

    private Map<String, ProductTool> toolMap() {
        Map<String, ProductTool> map = new LinkedHashMap<>();
        for (ProductTool tool : tools) {
            map.putIfAbsent(tool.name(), tool);
        }
        return map;
    }

    private static ToolCall parseToolCall(String answer) {
        if (answer == null) {
            return null;
        }
        String trimmed = answer.trim();
        ToolCall structured = parseStructuredToolCall(trimmed);
        if (structured != null) {
            return structured;
        }
        String prefix = "tool_call:";
        if (!trimmed.startsWith(prefix)) {
            return null;
        }
        String body = trimmed.substring(prefix.length()).trim();
        int split = body.indexOf('|');
        if (split <= 0 || split >= body.length() - 1) {
            return null;
        }
        String toolName = body.substring(0, split).trim();
        String input = body.substring(split + 1).trim();
        if (toolName.isEmpty()) {
            return null;
        }
        return new ToolCall(toolName, input);
    }

    /**
     * 解析结构化工具调用格式，支持 JSON 与 XML 两种常见输出。
     */
    private static ToolCall parseStructuredToolCall(String text) {
        if (text.startsWith("{") && text.contains("\"tool_call\"")) {
            String name = jsonValue(text, "\"name\"");
            String input = jsonValue(text, "\"input\"");
            if (name != null && !name.isBlank()) {
                return new ToolCall(name.trim(), input == null ? "" : input.trim());
            }
        }
        if (text.contains("<tool_call>")) {
            String name = xmlValue(text, "name");
            String input = xmlValue(text, "input");
            if (name != null && !name.isBlank()) {
                return new ToolCall(name.trim(), input == null ? "" : input.trim());
            }
        }
        return null;
    }

    private static String jsonValue(String body, String key) {
        int keyIdx = body.indexOf(key);
        if (keyIdx < 0) {
            return null;
        }
        int colon = body.indexOf(':', keyIdx + key.length());
        if (colon < 0) {
            return null;
        }
        int start = body.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        int end = body.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return body.substring(start + 1, end);
    }

    private static String xmlValue(String body, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = body.indexOf(open);
        int end = body.indexOf(close);
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        return body.substring(start + open.length(), end);
    }

    /**
     * 限制工具名字符集，避免模型构造危险标识符。
     */
    private static boolean isSafeToolName(String toolName) {
        return toolName != null && toolName.matches("[a-zA-Z0-9_\\-]{1,64}");
    }

    /**
     * 限制工具输入长度并过滤高风险片段，降低注入风险。
     */
    private static boolean isSafeToolInput(String input) {
        if (input == null) {
            return true;
        }
        if (input.length() > 4000) {
            return false;
        }
        String lower = input.toLowerCase(Locale.ROOT);
        return !lower.contains("<script") && !lower.contains("javascript:");
    }

    private record ToolCall(String toolName, String input) {
    }

    private record ModelRound(String answer, TokenUsageDto usage) {
    }
}
