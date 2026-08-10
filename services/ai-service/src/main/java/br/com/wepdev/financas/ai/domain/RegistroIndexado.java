package br.com.wepdev.financas.ai.domain;

import java.util.List;
import java.util.UUID;

/** Um ponto no vector store — hoje só descrição de transação (ai-strategy.md seção 2), extensível pra lançamento/orçamento depois. */
public record RegistroIndexado(UUID id, UUID usuarioId, String texto, List<Float> vetor) {
}
