package io.github.alcq77.cqgent.product.sdk.example;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.product.core.rag.*;
import io.github.alcq77.cqgent.product.core.runtime.advisor.RagContextAdvisor;
import io.github.alcq77.cqgent.product.sdk.AgentClient;
import io.github.alcq77.cqgent.product.sdk.AgentClientBuilder;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;

import java.util.List;
import java.util.Map;

/**
 * 本地知识库问答最小示例。
 */
public final class RagQuickStartExample {

    private RagQuickStartExample() {
    }

    public static void main(String[] args) {
        InMemoryRagStore store = new InMemoryRagStore();
        TextEmbeddingModel embeddingModel = new SimpleHashEmbeddingModel(128);
        RagIndexer indexer = new RagIndexer(new RagChunkSplitter(400, 80), embeddingModel, store);
        indexer.rebuild(List.of(
            new RagDocument("doc-1", "退款规则", "订单支付后 7 天内支持无理由退款，超过 7 天需人工审核。", Map.of("topic", "refund")),
            new RagDocument("doc-2", "会员规则", "VIP 用户享受 95 折优惠，续费后即时生效。", Map.of("topic", "vip"))
        ));

        RagRetriever retriever = new RagRetriever(store, embeddingModel);
        RagContextAdvisor advisor = new RagContextAdvisor(retriever, 2, -100);

        AgentClient client = AgentClientBuilder.create()
            .logicalModel("primary-llm")
            .endpoint(ProductEndpointConfig.builder()
                .id("primary")
                .provider("openai_compat")
                .baseUrl("http://127.0.0.1:11434")
                .defaultModel("llama3.2")
                .build())
            .route("primary-llm", "primary")
            .advisor(advisor)
            .build();

        AgentChatRequest request = AgentChatRequest.builder()
            .message("我的订单已经 9 天了，还能直接退款吗？")
            .taskType("qa")
            .tags(List.of("domain:ecommerce"))
            .build();
        System.out.println(client.chat(request).getReply());
    }
}
