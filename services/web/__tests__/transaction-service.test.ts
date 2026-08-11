import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  TransactionServiceError,
  criarTransacao,
  criarTransacaoRecorrente,
  listarTransacoes,
} from "@/lib/transaction-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("listarTransacoes", () => {
  test("lança 401 sem access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);
    await expect(listarTransacoes()).rejects.toMatchObject({ status: 401 });
  });

  test("monta a query string só com os filtros informados", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => [] });
    vi.stubGlobal("fetch", fetchMock);

    await listarTransacoes({ contaId: "conta-1" });

    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8082/api/v1/transacoes?contaId=conta-1");
  });

  test("sem filtro nenhum não anexa '?'", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => [] });
    vi.stubGlobal("fetch", fetchMock);

    await listarTransacoes();

    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8082/api/v1/transacoes");
  });

  test("propaga a mensagem de erro do corpo da resposta", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ mensagem: "inicio depois de fim" }),
      })
    );

    await expect(listarTransacoes({ inicio: "2026-08-01" })).rejects.toThrow(
      "inicio depois de fim"
    );
  });
});

describe("criarTransacao", () => {
  test("lança TransactionServiceError com status 422 (saldo insuficiente)", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Saldo insuficiente" }),
      })
    );

    await expect(
      criarTransacao({
        contaId: "conta-1",
        descricao: "Mercado",
        valor: 100,
        tipo: "DESPESA",
      })
    ).rejects.toThrow(TransactionServiceError);
  });
});

describe("criarTransacaoRecorrente", () => {
  test("envia frequencia MENSAL no corpo", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "1" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await criarTransacaoRecorrente({
      contaId: "conta-1",
      descricao: "Salário",
      valor: 5000,
      tipo: "RECEITA",
      frequencia: "MENSAL",
      dataInicio: "2026-08-01",
    });

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(init.body).frequencia).toBe("MENSAL");
  });
});
