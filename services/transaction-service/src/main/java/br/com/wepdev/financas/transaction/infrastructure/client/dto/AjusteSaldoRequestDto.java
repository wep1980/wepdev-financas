package br.com.wepdev.financas.transaction.infrastructure.client.dto;

import java.math.BigDecimal;

public record AjusteSaldoRequestDto(BigDecimal valor) {
}
