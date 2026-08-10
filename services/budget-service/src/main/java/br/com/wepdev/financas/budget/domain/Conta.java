package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Leitura do account-service (ADR-0026) — só os campos que o cálculo de disponível pra gastar precisa. */
public record Conta(UUID id, String nome, String tipo, BigDecimal saldo) {
}
