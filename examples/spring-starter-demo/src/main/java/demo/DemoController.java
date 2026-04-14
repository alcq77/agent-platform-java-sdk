package demo;

import io.github.alcq77.cqgent.agent.api.dto.AgentChatRequest;
import io.github.alcq77.cqgent.product.sdk.AgentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final AgentClient agentClient;

    @GetMapping("/demo/chat")
    public String chat(@RequestParam(defaultValue = "你好") String q) {
        return agentClient.chat(AgentChatRequest.builder().message(q).build()).getReply();
    }
}
