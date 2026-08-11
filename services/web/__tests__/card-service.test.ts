import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  CardServiceError,
  criarCartao,
  listarCartoes,
  listarCompras,
} from "@/lib/card-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("listarCartoes", () => {
  test("lança CardServiceError 401 se não houver access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);

    await expect(listarCartoes()).rejects.toMatchObject({ status: 401 });
  });

  test("propaga o Authorization: Bearer e devolve a lista", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "1", apelido: "Nubank" }],
    });
    vi.stubGlobal("fetch", fetchMock);

    const cartoes = await listarCartoes();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8083/api/v1/cartoes",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer token-abc" }),
      })
    );
    expect(cartoes).toEqual([{ id: "1", apelido: "Nubank" }]);
  });
});

describe("criarCartao", () => {
  test("envia o corpo serializado em JSON", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "1", apelido: "Nubank" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await criarCartao({
      apelido: "Nubank",
      bandeira: "MASTERCARD",
      limite: 5000,
      diaFechamento: 10,
      diaVencimento: 20,
      contaPagamentoId: "conta-1",
    });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8083/api/v1/cartoes");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      apelido: "Nubank",
      bandeira: "MASTERCARD",
      limite: 5000,
      diaFechamento: 10,
      diaVencimento: 20,
      contaPagamentoId: "conta-1",
    });
  });

  test("lança CardServiceError com o status HTTP original", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Conta de pagamento não encontrada" }),
      })
    );

    await expect(
      criarCartao({
        apelido: "X",
        bandeira: "VISA",
        limite: 100,
        diaFechamento: 1,
        diaVencimento: 10,
        contaPagamentoId: "conta-inexistente",
      })
    ).rejects.toThrow(CardServiceError);
  });
});

describe("listarCompras", () => {
  test("busca as compras do cartão", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ compraId: "c1", descricao: "Notebook" }],
    });
    vi.stubGlobal("fetch", fetchMock);

    const compras = await listarCompras("cartao-1");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8083/api/v1/cartoes/cartao-1/compras",
      expect.anything()
    );
    expect(compras).toEqual([{ compraId: "c1", descricao: "Notebook" }]);
  });
});
