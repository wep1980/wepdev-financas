package br.com.wepdev.financas.budget.infrastructure.client.dto;

import java.math.BigDecimal;

/** Só os campos que este serviço precisa da resposta do transaction-service — não é o contrato inteiro. */
public record ResumoCategoriaDto(String categoria, BigDecimal totalGasto) {
}
