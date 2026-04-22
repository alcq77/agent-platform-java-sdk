package io.github.alcq77.cqagent.product.spi.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;

/**
 * 模型提供方 SPI。
 * <p>
 * 设计目标：
 * - 对外统一返回 LangChain4j 的 {@link ChatLanguageModel}；
 * - 由 provider 负责把 endpoint 配置转换为可运行模型实例；
 * - 上层 runtime 不再感知厂商协议细节。
 */
public interface ProductModelProvider {

    /**
     * provider 唯一编码（如 openai_compat）。
     */
    String providerCode();

    /**
     * 按 endpoint + logicalModel 构建可调用模型。
     *
     * @param endpoint     路由后的物理端点配置
     * @param logicalModel 逻辑模型名（用于多模型映射与回退）
     * @return LangChain4j 聊天模型实例
     */
    ChatLanguageModel createChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel);

    /**
     * 构建流式聊天模型；默认实现可选择不支持。
     */
    default StreamingChatLanguageModel createStreamingChatLanguageModel(ProductEndpointConfig endpoint, String logicalModel) {
        throw new UnsupportedOperationException("streaming model is not supported by provider: " + providerCode());
    }
}
