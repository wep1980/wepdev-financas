package br.com.wepdev.financas.ai.domain;

import java.util.List;

/** Vetor de embedding pronto pra indexar/consultar no Qdrant (RAG, item 7). */
public record EmbeddingResult(List<Float> vetor) {
}
