package io.github.alcq77.cqagent.product.core.rag;

/**
 * 文本 embedding 抽象。
 */
public interface TextEmbeddingModel {

    double[] embed(String text);
}
