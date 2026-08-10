package br.com.wepdev.financas.ai.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** usuarioId não vem aqui — é extraído do token (sub), nunca confiado do cliente (ADR-0003). */
public record ChatRequest(
        UUID conversaId,
        @NotBlank String mensagem
) {
}
