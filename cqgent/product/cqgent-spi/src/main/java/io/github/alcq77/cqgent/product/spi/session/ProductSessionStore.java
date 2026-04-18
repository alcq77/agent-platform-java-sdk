package io.github.alcq77.cqgent.product.spi.session;

import io.github.alcq77.cqgent.model.api.dto.ChatMessageDto;

import java.util.List;

public interface ProductSessionStore {

    /**
     * 判断会话是否已存在。
     */
    boolean hasSession(String sessionId);

    /**
     * 注册会话（允许幂等）。
     */
    void register(String sessionId);

    /**
     * 读取会话历史（按时间顺序）。
     */
    List<ChatMessageDto> history(String sessionId);

    /**
     * 追加一轮用户/助手消息。
     */
    void append(String sessionId, ChatMessageDto user, ChatMessageDto assistant);

    /**
     * 用完整历史覆盖会话。
     * <p>
     * 主要用于 LangChain4j ChatMemoryStore 的同步写入。
     */
    default void replaceHistory(String sessionId, List<ChatMessageDto> messages) {
        throw new UnsupportedOperationException("replaceHistory is not supported by " + getClass().getName());
    }

    /**
     * 删除会话及其历史。
     */
    default void deleteSession(String sessionId) {
        throw new UnsupportedOperationException("deleteSession is not supported by " + getClass().getName());
    }
}
