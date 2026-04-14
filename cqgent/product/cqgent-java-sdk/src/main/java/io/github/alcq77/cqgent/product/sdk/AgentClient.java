package io.github.alcq77.cqgent.product.sdk;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.agent.api.dto.AgentChatResponse;

public interface AgentClient {

    AgentChatResponse chat(AgentChatRequest request);
}
