package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import java.util.List;

public record QdrantUpsertPontosRequestDto(List<QdrantPontoDto> points) {
}
