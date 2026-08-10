package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Parâmetros da tool de escrita criar_transacao (PRD 3.5) pra uma regra recorrente — já confirmada pelo usuário (ADR-0007) quando isso é chamado. */
public record CriarTransacaoRecorrenteComando(UUID contaId, String descricao, BigDecimal valor, TipoTransacao tipo,
                                               String categoria, FrequenciaRecorrencia frequencia,
                                               LocalDate dataInicio, Integer quantidadeOcorrencias) {
}
