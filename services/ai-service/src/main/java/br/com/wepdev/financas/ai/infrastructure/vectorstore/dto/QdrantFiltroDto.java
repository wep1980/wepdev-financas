package br.com.wepdev.financas.ai.infrastructure.vectorstore.dto;

import java.util.List;

/** {@code must}: todas as condições precisam bater — usado pra filtrar por usuarioId (isolamento multi-tenant, ADR-0003). */
public record QdrantFiltroDto(List<QdrantCondicaoDto> must) {
}
