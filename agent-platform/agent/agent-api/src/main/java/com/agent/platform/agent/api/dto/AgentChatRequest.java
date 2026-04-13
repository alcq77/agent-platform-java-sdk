package com.agent.platform.agent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
