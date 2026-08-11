package br.com.wepdev.financas.document.domain;

import java.math.BigDecimal;

/**
 * Assinatura de uma compra já lançada no card-service (ver
 * {@link CardServiceClient#listarComprasAtivas(java.util.UUID)}) — só os
 * campos usados pro casamento de dedup em {@code ConfirmarLancamentosUseCase}
 * (ADR-0028). Não é a compra inteira (card-service tem mais campos, ex:
 * parcelas restantes) — o document-service só precisa saber "já existe
 * algo assim?", não os detalhes.
 *
 * <p>Sem {@code quantidadeParcelas} de propósito (achado real,
 * 2026-08-11): o card-service guarda só as parcelas RESTANTES quando a
 * compra é registrada no meio de uma sequência, então esse número não é
 * estável entre uploads — só descrição-base + valor da parcela são
 * comparáveis com segurança (ver {@code ConfirmarLancamentosUseCase.jaExiste}).
 */
public record CompraExistente(String descricao, BigDecimal valorParcela) {
}
