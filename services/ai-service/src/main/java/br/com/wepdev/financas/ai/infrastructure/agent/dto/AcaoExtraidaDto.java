package br.com.wepdev.financas.ai.infrastructure.agent.dto;

import java.math.BigDecimal;

/** Shape cru do JSON pedido ao LLM na extração de parâmetros de ação — nunca confiado sem validação, ver AgenteOrquestradorUseCase. */
public record AcaoExtraidaDto(String tipo, String descricao, BigDecimal valor, Boolean recorrente,
                               Integer quantidadeOcorrencias, String categoria, String contaTexto) {
}
