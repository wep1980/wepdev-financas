package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import java.util.List;
import java.util.Map;

/** Corpo de PUT /collections/{nome}/points — só os campos que este serviço usa. */
public record QdrantPontoDto(String id, List<Float> vector, Map<String, Object> payload) {
}
