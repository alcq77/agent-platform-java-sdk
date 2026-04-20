package io.github.alcq77.cqgent.product.core.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存知识块存储。
 */
public class InMemoryRagStore {

    private final List<RagChunk> chunks = new CopyOnWriteArrayList<>();

    public void replaceAll(List<RagChunk> newChunks) {
        chunks.clear();
        if (newChunks != null) {
            chunks.addAll(newChunks);
        }
    }

    public List<RagChunk> all() {
        return new ArrayList<>(chunks);
    }
}
