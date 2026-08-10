package br.com.wepdev.financas.budget.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do account-service — não é o contrato inteiro. */
public record ContaDto(UUID id, String nome, String tipo, BigDecimal saldo) {
}
