package br.com.wepdev.financas.ai.infrastructure.llm.dto;

import java.util.List;

/** Resposta de POST /api/embeddings — só os campos que este serviço usa. */
public record OllamaEmbeddingsResponseDto(List<Float> embedding) {
}
