package io.github.alcq77.cqagent.product.sdk.provider;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.github.alcq77.cqagent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqagent.product.spi.model.ProductModelProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 LangChain4j OpenAI 模型实现的 provider。
 * <p>
 * 说明：
 * - providerCode 保持 openai_compat，不破坏已有配置；
 * - 模型实例由 OpenAiChatModel 原生 builder 构建；
 * - endpoint 透传 custom headers，以兼容 OpenAI-compatible 服务。
 */
public class OpenAiCompatibleProductProvider implements ProductModelProvider {

    @Override
    public String providerCode() {
        return "openai_compat";
    }

    @Override
    public ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String modelName = endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
            .baseUrl(normalizedBaseUrl(endpoint.getBaseUrl()))
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        return builder.build();
    }

    @Override
    public StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        String modelName = endpoint.getDefaultModel() == null || endpoint.getDefaultModel().isBlank()
            ? logicalModel
            : endpoint.getDefaultModel();
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
            .baseUrl(normalizedBaseUrl(endpoint.getBaseUrl()))
            .modelName(modelName)
            .timeout(endpoint.getReadTimeout())
            .customHeaders(safeHeaders(endpoint.getHeaders()));
        if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
            builder.apiKey(endpoint.getApiKey());
        }
        return builder.build();
    }

    /**
     * 统一去掉末尾斜杠，避免依赖方重复拼接路径导致 404。
     */
    private static String normalizedBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            throw new IllegalArgumentException("endpoint.baseUrl must not be blank");
        }
        String value = rawBaseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 拷贝 headers，规避外部可变 Map 带来的并发风险。
     */
    private static Map<String, String> safeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(headers);
    }
}
