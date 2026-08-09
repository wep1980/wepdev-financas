package br.com.wepdev.financas.card.infrastructure.client.dto;

import java.math.BigDecimal;

public record AjusteSaldoRequestDto(BigDecimal valor) {
}
