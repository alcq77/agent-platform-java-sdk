package io.github.alcq77.cqgent.product.core.session;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import io.github.alcq77.cqgent.product.spi.session.ProductSessionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于 ProductSessionStore 的 LangChain4j ChatMemory 适配器。
 */
public class ProductSessionChatMemory implements ChatMemory {

    /**
     * sessionId 同时作为 memoryId。
     */
    private final String sessionId;
    /**
     * 负责与 ProductSessionStore 做双向转换。
     */
    private final ProductSessionStoreChatMemoryStore store;
    /**
     * 当前会话内存快照（当前请求周期内可变）。
     */
    private final List<ChatMessage> messages;

    public ProductSessionChatMemory(String sessionId, ProductSessionStore sessionStore) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.store = new ProductSessionStoreChatMemoryStore(Objects.requireNonNull(sessionStore, "sessionStore"));
        this.messages = new ArrayList<>(store.getMessages(sessionId));
    }

    @Override
    public Object id() {
        return sessionId;
    }

    @Override
    public void add(ChatMessage message) {
        messages.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        return new ArrayList<>(messages);
    }

    @Override
    public void clear() {
        messages.clear();
        store.deleteMessages(sessionId);
    }

    public void syncToStore() {
        // 请求结束后统一刷盘，避免每条消息都写存储造成放大。
        store.updateMessages(sessionId, messages);
    }
}
