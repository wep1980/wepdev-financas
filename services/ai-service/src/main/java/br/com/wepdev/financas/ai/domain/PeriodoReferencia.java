package br.com.wepdev.financas.ai.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Período que uma pergunta de consulta pode se referir — classificado
 * pelo LLM (um rótulo simples, não uma data), a aritmética de data em si
 * é sempre feita aqui em código Java determinístico, nunca pedida ao
 * LLM (mesma lição já aprendida no document-service: "nunca pedir
 * aritmética de data ao LLM", ver docs/historico.md 2026-08-09).
 */
public enum PeriodoReferencia {
    MES_ATUAL,
    MES_PASSADO,
    ULTIMOS_3_MESES;

    /** Usado pela tool buscar_saldo_disponivel (budget-service trabalha por mês, não por intervalo). */
    public YearMonth mesReferencia() {
        YearMonth agora = YearMonth.now();
        return switch (this) {
            case MES_ATUAL, ULTIMOS_3_MESES -> agora;
            case MES_PASSADO -> agora.minusMonths(1);
        };
    }

    public LocalDate inicio() {
        YearMonth agora = YearMonth.now();
        return switch (this) {
            case MES_ATUAL -> agora.atDay(1);
            case MES_PASSADO -> agora.minusMonths(1).atDay(1);
            case ULTIMOS_3_MESES -> agora.minusMonths(2).atDay(1);
        };
    }

    public LocalDate fim() {
        return switch (this) {
            case MES_ATUAL, ULTIMOS_3_MESES -> LocalDate.now();
            case MES_PASSADO -> YearMonth.now().minusMonths(1).atEndOfMonth();
        };
    }
}
