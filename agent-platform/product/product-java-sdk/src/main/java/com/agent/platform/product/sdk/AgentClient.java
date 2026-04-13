package com.agent.platform.product.sdk;

import com.agent.platform.agent.api.dto.AgentChatRequest;
import com.agent.platform.agent.api.dto.AgentChatResponse;

public interface AgentClient {

    AgentChatResponse chat(AgentChatRequest request);
}
