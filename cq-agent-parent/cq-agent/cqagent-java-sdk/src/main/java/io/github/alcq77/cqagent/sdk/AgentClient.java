package io.github.alcq77.cqagent.sdk;

import io.github.alcq77.cqagent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqagent.agent.api.dto.AgentChatResponse;

/**
 * SDK 对外最小调用接口。
 */
public interface AgentClient {

    /**
     * 同步问答调用。
     */
    AgentChatResponse chat(AgentChatRequest request);

    /**
     * 流式对话接口。
     */
    default void stream(AgentChatRequest request, AgentStreamingListener listener) {
        throw new UnsupportedOperationException("stream is not supported by " + getClass().getName());
    }
}
