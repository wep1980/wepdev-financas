package br.com.wepdev.financas.ai.infrastructure.llm.dto;

/** Corpo de POST /v1/embeddings — só os campos que este serviço usa. */
public record OpenAiEmbeddingRequestDto(String model, String input) {
}
