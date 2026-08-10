package br.com.wepdev.financas.ai.infrastructure.agent.dto;

/** Shape cru do JSON pedido ao LLM na classificação de intenção — nunca confiado sem validação, ver AgenteOrquestradorUseCase. */
public record IntencaoDetectadaDto(String intent, String tool, String periodo) {
}
