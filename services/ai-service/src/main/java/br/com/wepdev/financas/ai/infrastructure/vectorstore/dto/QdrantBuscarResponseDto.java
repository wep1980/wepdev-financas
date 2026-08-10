package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import java.util.List;

public record QdrantBuscarResponseDto(List<QdrantResultadoDto> result) {
}
