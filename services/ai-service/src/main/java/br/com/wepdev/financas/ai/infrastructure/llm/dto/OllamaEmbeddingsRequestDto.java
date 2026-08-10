package br.com.wepdev.financas.ai.infrastructure.llm.dto;

/** Corpo de POST /api/embeddings — só os campos que este serviço usa. */
public record OllamaEmbeddingsRequestDto(String model, String prompt) {
}
