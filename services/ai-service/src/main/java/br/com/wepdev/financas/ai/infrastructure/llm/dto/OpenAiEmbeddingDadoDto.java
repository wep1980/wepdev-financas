package br.com.wepdev.financas.ai.infrastructure.llm.dto;

import java.util.List;

public record OpenAiEmbeddingDadoDto(List<Float> embedding) {
}
