import type { Transacao } from "@/lib/transaction-service";

export const MESES_MEDIA_RECEITA = 3;

/**
 * Reserva sugerida = média mensal de RECEITA confirmada nos últimos
 * MESES_MEDIA_RECEITA meses — regra de "1 mês de renda de colchão",
 * decisão do usuário (2026-08-10). `transacoes` já deve vir filtrada
 * pela janela de meses (ver lib/mes.ts limitesUltimosMeses) — esta
 * função só soma e divide, não sabe nada de datas.
 */
export function calcularReservaSugerida(
  transacoes: Transacao[],
  quantidadeMeses: number = MESES_MEDIA_RECEITA
): number {
  const totalReceitas = transacoes
    .filter((t) => t.tipo === "RECEITA" && t.status === "CONFIRMADA")
    .reduce((soma, t) => soma + t.valor, 0);

  return totalReceitas / quantidadeMeses;
}
