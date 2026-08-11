import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  BudgetServiceError,
  buscarDisponivelParaGastar,
  criarOrcamento,
  definirReserva,
} from "@/lib/budget-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("buscarDisponivelParaGastar", () => {
  test("lança 401 sem access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);
    await expect(buscarDisponivelParaGastar("2026-08")).rejects.toMatchObject({
      status: 401,
    });
  });

  test("chama o endpoint com o mês informado", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ mesReferencia: "2026-08", valorDisponivel: 100 }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await buscarDisponivelParaGastar("2026-08");

    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe(
      "http://localhost:8085/api/v1/disponivel-para-gastar?mes=2026-08"
    );
  });

  test("propaga a mensagem de erro do corpo da resposta", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ mensagem: "mes fora do formato AAAA-MM" }),
      })
    );

    await expect(buscarDisponivelParaGastar("agosto")).rejects.toThrow(
      "mes fora do formato AAAA-MM"
    );
  });
});

describe("criarOrcamento", () => {
  test("lança BudgetServiceError 422 quando já existe orçamento ativo", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Já existe orçamento ativo" }),
      })
    );

    await expect(
      criarOrcamento({
        categoria: "Mercado",
        mesReferencia: "2026-08",
        valorLimite: 800,
      })
    ).rejects.toThrow(BudgetServiceError);
  });
});

describe("definirReserva", () => {
  test("envia o valor no corpo da requisição PUT", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ usuarioId: "u1", valor: 500, atualizadoEm: null }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await definirReserva(500);

    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body)).toEqual({ valor: 500 });
  });
});
