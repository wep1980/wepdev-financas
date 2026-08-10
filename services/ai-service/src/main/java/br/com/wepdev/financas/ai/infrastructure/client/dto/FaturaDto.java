package br.com.wepdev.financas.ai.infrastructure.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Só os campos que este serviço precisa da resposta do card-service — não é o contrato inteiro. */
public record FaturaDto(UUID id, YearMonth competencia, LocalDate dataVencimento, BigDecimal valorTotal, String status) {
}
