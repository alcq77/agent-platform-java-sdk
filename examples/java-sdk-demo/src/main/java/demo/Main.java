package demo;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.product.sdk.AgentClient;
import io.github.alcq77.cqgent.product.sdk.AgentClientBuilder;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;

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
