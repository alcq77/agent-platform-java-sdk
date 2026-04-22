package io.github.alcq77.cqagent.product.core.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
        return retrieve(query, topK, null);
    }

    public List<RagChunk> retrieve(String query, int topK, RagRetrievalFilter filter) {
        double[] q = embeddingModel.embed(query);
        List<ScoredChunk> scored = new ArrayList<>();
        for (RagChunk chunk : store.all()) {
            if (!matchedByFilter(chunk, filter)) {
                continue;
            }
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

    private static boolean matchedByFilter(RagChunk chunk, RagRetrievalFilter filter) {
        if (filter == null) {
            return true;
        }
        Map<String, String> metadata = chunk.metadata() == null ? Map.of() : chunk.metadata();
        for (Map.Entry<String, String> entry : filter.getEqualsMetadata().entrySet()) {
            String actual = metadata.get(entry.getKey());
            if (actual == null || !actual.equals(entry.getValue())) {
                return false;
            }
        }
        if (!filter.getAllowedSources().isEmpty()) {
            String source = metadata.get("source");
            if (source == null || filter.getAllowedSources().stream().noneMatch(source::contains)) {
                return false;
            }
        }
        return true;
    }

    private record ScoredChunk(RagChunk chunk, double score) {
    }
}
