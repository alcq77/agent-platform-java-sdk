package com.agent.platform.product.sdk.example;

import com.agent.platform.agent.api.dto.AgentChatRequest;
import com.agent.platform.product.sdk.AgentClient;
import com.agent.platform.product.sdk.AgentClientBuilder;
import com.agent.platform.product.spi.model.ProductEndpointConfig;

public final class SdkQuickStartExample {

    private SdkQuickStartExample() {
    }

    public static void main(String[] args) {
        AgentClient client = AgentClientBuilder.create()
                .logicalModel("primary-llm")
                .endpoint(ProductEndpointConfig.builder()
                        .id("primary")
                        .provider("openai_compat")
                        .baseUrl("http://127.0.0.1:11434")
                        .defaultModel("llama3.2")
                        .build())
                .route("primary-llm", "primary")
                .build();
        System.out.println(client.chat(AgentChatRequest.builder().message("你好").build()).getReply());
    }
}
