package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corpo de POST /collections/{nome}/points/search — só os campos que este serviço usa. */
public record QdrantBuscarRequestDto(List<Float> vector, int limit, QdrantFiltroDto filter,
                                      @JsonProperty("with_payload") boolean comPayload) {
}
