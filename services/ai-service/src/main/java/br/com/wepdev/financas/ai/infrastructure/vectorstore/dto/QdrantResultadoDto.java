package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import java.util.Map;

/** Só os campos que este serviço usa da resposta do Qdrant. */
public record QdrantResultadoDto(String id, float score, Map<String, Object> payload) {
}
