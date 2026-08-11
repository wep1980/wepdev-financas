import { afterEach, describe, expect, test, vi } from "vitest";
import { buscarCotacaoDolar } from "@/lib/cambio";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("buscarCotacaoDolar", () => {
  test("converte bid/ask pra compra/venda numéricos", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          USDBRL: {
            bid: "5.4100",
            ask: "5.4120",
            create_date: "2026-08-10 15:00:00",
          },
        }),
      })
    );

    const cotacao = await buscarCotacaoDolar();

    expect(cotacao).toEqual({
      compra: 5.41,
      venda: 5.412,
      dataHora: "2026-08-10 15:00:00",
    });
  });

  test("devolve null se a resposta não for ok", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false }));
    expect(await buscarCotacaoDolar()).toBeNull();
  });

  test("devolve null se a chamada falhar (rede fora do ar)", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new Error("network error"))
    );
    expect(await buscarCotacaoDolar()).toBeNull();
  });
});
