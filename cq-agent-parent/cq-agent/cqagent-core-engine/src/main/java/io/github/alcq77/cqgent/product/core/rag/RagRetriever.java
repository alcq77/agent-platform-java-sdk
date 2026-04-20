package io.github.alcq77.cqgent.product.core.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 向量检索器（余弦相似度）。
 */
public class RagRetriever {

    private final InMemoryRagStore store;
    private final TextEmbeddingModel embeddingModel;

    public RagRetriever(InMemoryRagStore store, TextEmbeddingModel embeddingModel) {
        this.store = store;
        this.embeddingModel = embeddingModel;
    }

    public List<RagChunk> retrieve(String query, int topK) {
        double[] q = embeddingModel.embed(query);
        List<ScoredChunk> scored = new ArrayList<>();
        for (RagChunk chunk : store.all()) {
            scored.add(new ScoredChunk(chunk, cosine(q, chunk.embedding())));
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        List<RagChunk> result = new ArrayList<>();
        int limit = Math.max(1, topK);
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            result.add(scored.get(i).chunk());
        }
        return result;
    }

    private static double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    private record ScoredChunk(RagChunk chunk, double score) {
    }
}
