package br.com.wepdev.financas.ai.infrastructure.llm.dto;

import java.util.List;

/** Resposta de POST /v1/embeddings — só os campos que este serviço usa. */
public record OpenAiEmbeddingResponseDto(List<OpenAiEmbeddingDadoDto> data) {
}
