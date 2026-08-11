import { describe, expect, test } from "vitest";
import { calcularReservaSugerida } from "@/lib/reserva-sugerida";
import type { Transacao } from "@/lib/transaction-service";

function transacao(overrides: Partial<Transacao>): Transacao {
  return {
    id: "t1",
    contaId: "c1",
    usuarioId: "u1",
    descricao: "x",
    valor: 0,
    tipo: "RECEITA",
    dataTransacao: "2026-08-01",
    status: "CONFIRMADA",
    criadoEm: "2026-08-01T00:00:00Z",
    ...overrides,
  };
}

describe("calcularReservaSugerida", () => {
  test("soma só RECEITA confirmada e divide pela quantidade de meses", () => {
    const transacoes = [
      transacao({ valor: 3000 }),
      transacao({ valor: 3000 }),
      transacao({ valor: 3000 }),
    ];

    expect(calcularReservaSugerida(transacoes, 3)).toBe(3000);
  });

  test("ignora DESPESA", () => {
    const transacoes = [
      transacao({ valor: 3000, tipo: "RECEITA" }),
      transacao({ valor: 500, tipo: "DESPESA" }),
    ];

    expect(calcularReservaSugerida(transacoes, 1)).toBe(3000);
  });

  test("ignora transação CANCELADA", () => {
    const transacoes = [
      transacao({ valor: 3000, status: "CONFIRMADA" }),
      transacao({ valor: 3000, status: "CANCELADA" }),
    ];

    expect(calcularReservaSugerida(transacoes, 1)).toBe(3000);
  });

  test("sem receita nenhuma dá zero", () => {
    expect(calcularReservaSugerida([], 3)).toBe(0);
  });
});
