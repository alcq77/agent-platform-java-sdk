package io.github.alcq77.cqgent.product.core.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 建索引入口：文档导入 + 切分 + embedding。
 */
public class RagIndexer {

    private final RagChunkSplitter splitter;
    private final TextEmbeddingModel embeddingModel;
    private final InMemoryRagStore store;

    public RagIndexer(RagChunkSplitter splitter, TextEmbeddingModel embeddingModel, InMemoryRagStore store) {
        this.splitter = splitter;
        this.embeddingModel = embeddingModel;
        this.store = store;
    }

    public void rebuild(List<RagDocument> documents) {
        List<RagChunk> chunks = new ArrayList<>();
        if (documents != null) {
            for (RagDocument document : documents) {
                chunks.addAll(splitter.split(document, embeddingModel));
            }
        }
        store.replaceAll(chunks);
    }
}
