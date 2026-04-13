package com.agent.platform.product.spi.session;

import com.agent.platform.model.api.dto.ChatMessageDto;

import java.util.List;

public interface ProductSessionStore {

    boolean hasSession(String sessionId);

    void register(String sessionId);

    List<ChatMessageDto> history(String sessionId);

    void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant);
}
