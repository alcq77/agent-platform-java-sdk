package demo;

import com.agent.platform.agent.api.dto.AgentChatRequest;
import com.agent.platform.product.sdk.AgentClient;
import com.agent.platform.product.sdk.AgentClientBuilder;
import com.agent.platform.product.spi.model.ProductEndpointConfig;

public class Main {

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
        String q = args.length > 0 ? args[0] : "你好，请介绍一下你";
        System.out.println(client.chat(AgentChatRequest.builder().message(q).build()).getReply());
    }
}
