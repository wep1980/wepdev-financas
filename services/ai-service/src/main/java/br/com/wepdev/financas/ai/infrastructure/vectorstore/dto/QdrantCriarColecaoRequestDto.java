package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

/** Corpo de PUT /collections/{nome} — só os campos que este serviço usa, não o contrato inteiro do Qdrant. */
public record QdrantCriarColecaoRequestDto(QdrantVetorConfigDto vectors) {
}
