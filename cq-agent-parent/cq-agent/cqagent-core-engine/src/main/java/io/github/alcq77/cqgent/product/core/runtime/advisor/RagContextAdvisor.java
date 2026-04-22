package io.github.alcq77.cqgent.product.core.runtime.advisor;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.product.core.rag.RagChunk;
import io.github.alcq77.cqgent.product.core.rag.RagRetrievalFilter;
import io.github.alcq77.cqgent.product.core.rag.RagRetriever;

import java.util.List;

/**
 * 检索知识片段并注入到本轮用户消息。
 */
public class RagContextAdvisor implements AgentRuntimeAdvisor {

    private final RagRetriever retriever;
    private final int topK;
    private final int order;
    private final RagRetrievalFilter filter;

    public RagContextAdvisor(RagRetriever retriever, int topK, int order) {
        this(retriever, topK, order, null);
    }

    public RagContextAdvisor(RagRetriever retriever, int topK, int order, RagRetrievalFilter filter) {
        this.retriever = retriever;
        this.topK = Math.max(1, topK);
        this.order = order;
        this.filter = filter;
    }

    @Override
    public int order() {
        return order;
    }

    @Override
    public AgentChatRequest before(AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return request;
        }
        List<RagChunk> chunks = retriever.retrieve(request.getMessage(), topK, filter);
        if (chunks.isEmpty()) {
            return request;
        }
        StringBuilder context = new StringBuilder();
        context.append("以下是可参考的知识库片段：\n");
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk chunk = chunks.get(i);
            context.append("[").append(i + 1).append("] ").append(chunk.text()).append("\n");
        }
        AgentChatRequest copied = AgentChatRequest.builder()
            .sessionId(request.getSessionId())
            .message(context + "\n用户问题：" + request.getMessage())
            .systemPrompt(request.getSystemPrompt())
            .traceId(request.getTraceId())
            .promptTemplateId(request.getPromptTemplateId())
            .promptVariables(request.getPromptVariables())
            .taskType(request.getTaskType())
            .tags(request.getTags())
            .build();
        return copied;
    }
}
