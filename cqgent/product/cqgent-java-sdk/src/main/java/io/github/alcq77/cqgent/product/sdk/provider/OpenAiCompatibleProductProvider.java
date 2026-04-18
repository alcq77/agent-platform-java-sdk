package io.github.alcq77.cqgent.product.sdk.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.github.alcq77.cqgent.common.api.ApiResponse;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionRequest;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionResponse;
import io.github.alcq77.cqgent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqgent.model.api.dto.TokenUsageDto;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqgent.product.spi.model.ProductModelProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiCompatibleProductProvider implements ProductModelProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerCode() {
        return "openai_compat";
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        // provider 只负责构建模型适配器，运行时策略交由 EmbeddedAgentClient + runtime 处理。
        return new OpenAiCompatibleChatLanguageModel(endpoint, logicalModel, objectMapper);
    }

    /**
     * OpenAI-compatible endpoint 到 LangChain4j ChatLanguageModel 的桥接实现。
     */
    static class OpenAiCompatibleChatLanguageModel implements ChatLanguageModel {

        private final ProductEndpointConfig endpoint;
        private final String logicalModel;
        private final ObjectMapper objectMapper;

        OpenAiCompatibleChatLanguageModel(ProductEndpointConfig endpoint,
                                          String logicalModel,
                                          ObjectMapper objectMapper) {
            this.endpoint = endpoint;
            this.logicalModel = logicalModel;
            this.objectMapper = objectMapper;
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return doGenerate(messages, List.of());
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            return doGenerate(messages, toolSpecifications == null ? List.of() : toolSpecifications);
        }

        private Response<AiMessage> doGenerate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(endpoint.getConnectTimeout())
                        .build();
                String root = endpoint.getBaseUrl().endsWith("/")
                        ? endpoint.getBaseUrl().substring(0, endpoint.getBaseUrl().length() - 1)
                        : endpoint.getBaseUrl();
                // 仍使用 legacy DTO 与兼容接口通信，但对上层暴露 LangChain4j 抽象。
                ChatCompletionRequest request = ChatCompletionRequest.builder()
                        .model(endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
                                ? logicalModel : endpoint.getDefaultModel())
                        .messages(toDtoMessages(messages, toolSpecifications))
                        .build();
                String reqBody = objectMapper.writeValueAsString(request);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(root + "/v1/chat/completions"))
                        .timeout(endpoint.getReadTimeout())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody));
                if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
                    builder.header("Authorization", "Bearer " + endpoint.getApiKey());
                }
                for (Map.Entry<String, String> h : endpoint.getHeaders().entrySet()) {
                    builder.header(h.getKey(), h.getValue());
                }
                HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException("upstream http " + response.statusCode() + ": " + response.body());
                }
                ChatCompletionResponse completion = unwrapResponse(response.body());
                String text = firstText(completion);
                TokenUsage tokenUsage = toTokenUsage(completion.getUsage());
                ToolExecutionRequest toolRequest = parseToolRequest(text);
                if (toolRequest != null) {
                    // 当模型输出工具调用时，转换为 LangChain4j 的 tool request 语义。
                    return Response.from(AiMessage.from(List.of(toolRequest)), tokenUsage);
                }
                return Response.from(AiMessage.from(text), tokenUsage);
            } catch (Exception e) {
                throw new IllegalStateException("openai compatible invoke failed: " + e.getMessage(), e);
            }
        }

        private ChatCompletionResponse unwrapResponse(String bodyText) throws Exception {
            if (bodyText.contains("\"code\"") && bodyText.contains("\"data\"")) {
                ApiResponse<ChatCompletionResponse> wrapped = objectMapper.readValue(bodyText, new TypeReference<>() {
                });
                if (wrapped.getCode() != 0 || wrapped.getData() == null) {
                    throw new IllegalStateException("model service response error: " + wrapped.getMessage());
                }
                return wrapped.getData();
            }
            return objectMapper.readValue(bodyText, ChatCompletionResponse.class);
        }

        private List<ChatMessageDto> toDtoMessages(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            List<ChatMessageDto> out = new ArrayList<>();
            if (!toolSpecifications.isEmpty()) {
                // 给 OpenAI-compatible 模型注入统一工具调用提示词。
                out.add(ChatMessageDto.builder()
                        .role("system")
                        .content(toolGuidance(toolSpecifications))
                        .build());
            }
            for (ChatMessage message : messages) {
                ChatMessageDto dto = toDtoMessage(message);
                if (dto != null) {
                    out.add(dto);
                }
            }
            return out;
        }

        private ChatMessageDto toDtoMessage(ChatMessage message) {
            if (message instanceof SystemMessage systemMessage) {
                return ChatMessageDto.builder().role("system").content(systemMessage.text()).build();
            }
            if (message instanceof UserMessage userMessage) {
                return ChatMessageDto.builder().role("user").content(userMessage.singleText()).build();
            }
            if (message instanceof AiMessage aiMessage) {
                if (aiMessage.hasToolExecutionRequests()) {
                    return ChatMessageDto.builder()
                            .role("assistant")
                            .content(formatToolCalls(aiMessage.toolExecutionRequests()))
                            .build();
                }
                return ChatMessageDto.builder().role("assistant").content(aiMessage.text()).build();
            }
            if (message instanceof ToolExecutionResultMessage toolResultMessage) {
                return ChatMessageDto.builder()
                        .role("system")
                        .content("ToolResult(" + toolResultMessage.toolName() + "): " + toolResultMessage.text())
                        .build();
            }
            return null;
        }

        private String toolGuidance(List<ToolSpecification> toolSpecifications) {
            StringBuilder builder = new StringBuilder();
            builder.append("When a tool is needed, reply ONLY in JSON format: ")
                    .append("{\"tool_call\":{\"name\":\"<toolName>\",\"input\":\"<plain text input>\"}}. ");
            builder.append("Available tools: ");
            for (int i = 0; i < toolSpecifications.size(); i++) {
                ToolSpecification specification = toolSpecifications.get(i);
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(specification.name());
            }
            return builder.toString();
        }

        private String formatToolCalls(List<ToolExecutionRequest> requests) {
            if (requests == null || requests.isEmpty()) {
                return "";
            }
            ToolExecutionRequest request = requests.get(0);
            return "{\"tool_call\":{\"name\":\"" + request.name() + "\",\"input\":\"" + request.arguments() + "\"}}";
        }

        private String firstText(ChatCompletionResponse response) {
            if (response.getChoices() == null || response.getChoices().isEmpty()
                    || response.getChoices().get(0).getMessage() == null) {
                throw new IllegalStateException("model returned empty assistant message");
            }
            return response.getChoices().get(0).getMessage().getContent();
        }

        private TokenUsage toTokenUsage(TokenUsageDto usage) {
            if (usage == null) {
                return null;
            }
            return new TokenUsage(usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        }

        private ToolExecutionRequest parseToolRequest(String text) {
            if (text == null) {
                return null;
            }
            String trimmed = text.trim();
            if (!trimmed.startsWith("{") || !trimmed.contains("\"tool_call\"")) {
                return null;
            }
            String name = jsonValue(trimmed, "\"name\"");
            String input = jsonValue(trimmed, "\"input\"");
            if (name == null || name.isBlank()) {
                return null;
            }
            return ToolExecutionRequest.builder()
                    .id("tool-" + System.nanoTime())
                    .name(name.trim())
                    .arguments(objectToJson(Map.of("input", input == null ? "" : input)))
                    .build();
        }

        private String objectToJson(Object value) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception ex) {
                throw new IllegalStateException("failed to write tool arguments", ex);
            }
        }

        private String jsonValue(String body, String key) {
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
    }
}
