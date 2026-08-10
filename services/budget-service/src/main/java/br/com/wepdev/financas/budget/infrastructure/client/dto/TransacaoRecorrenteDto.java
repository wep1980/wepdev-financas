package br.com.wepdev.financas.budget.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do transaction-service — não é o contrato inteiro. */
public record TransacaoRecorrenteDto(UUID id, String descricao, BigDecimal valor, String tipo, LocalDate dataInicio) {
}
