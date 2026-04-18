package io.github.alcq77.cqgent.agent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 对话请求（同步）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentChatRequest {

    /**
     * 会话 ID；不传则创建新会话并随响应返回。
     */
    private String sessionId;

    /**
     * 本轮用户输入。
     */
    @NotBlank
    private String message;

    /**
     * 可选系统提示；每轮请求可覆盖，不写入会话历史。
     */
    private String systemPrompt;

    /**
     * 请求追踪 ID；不传时由运行时自动生成并回传。
     */
    private String traceId;

    /**
     * Prompt 模板 ID（例如: customer-support:v1）。
     */
    private String promptTemplateId;

    /**
     * Prompt 模板变量；用于替换 systemPrompt/message 中的 {{key}} 占位符。
     */
    @Builder.Default
    private Map<String, String> promptVariables = new LinkedHashMap<>();
}
