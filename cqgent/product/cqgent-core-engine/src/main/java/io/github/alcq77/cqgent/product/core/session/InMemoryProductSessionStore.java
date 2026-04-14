package io.github.alcq77.cqgent.product.core.session;

import io.github.alcq77.cqgent.model.api.dto.ChatMessageDto;
import io.github.alcq77.cqgent.product.spi.session.ProductSessionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProductSessionStore implements ProductSessionStore {

    private final Map<String, List<ChatMessageDto>> sessions = new ConcurrentHashMap<>();
    private final int maxHistoryMessages;

    public InMemoryProductSessionStore(int maxHistoryMessages) {
        this.maxHistoryMessages = Math.max(10, maxHistoryMessages);
    }

    @Override
    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    @Override
    public void register(String sessionId) {
        sessions.putIfAbsent(sessionId, new ArrayList<>());
    }

    @Override
    public List<ChatMessageDto> history(String sessionId) {
        List<ChatMessageDto> history = sessions.getOrDefault(sessionId, List.of());
        return new ArrayList<>(history);
    }

    @Override
    public void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant) {
        List<ChatMessageDto> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        history.add(user);
        history.add(assistant);
        int overflow = history.size() - maxHistoryMessages;
        if (overflow > 0) {
            history.subList(0, overflow).clear();
        }
    }
}
