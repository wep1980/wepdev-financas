package br.com.wepdev.financas.ai.domain;

import java.util.List;
import java.util.UUID;

/**
 * Porta de saída pro Qdrant (ADR-0005, RAG). {@code buscarSimilares}
 * sempre filtra por {@code usuarioId} do lado do vector store — nunca
 * devolve ponto de outro usuário, mesmo isolamento por usuário já
 * exigido em todo o resto do sistema (ADR-0003).
 */
public interface VectorStore {

    /** Upsert por id — reindexar o mesmo id substitui o vetor/texto anteriores. */
    void indexar(RegistroIndexado registro);

    List<ResultadoBusca> buscarSimilares(UUID usuarioId, List<Float> vetorConsulta, int limite);
}
