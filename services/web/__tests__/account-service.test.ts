import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  AccountServiceError,
  criarConta,
  listarContas,
} from "@/lib/account-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("listarContas", () => {
  test("lança AccountServiceError 401 se não houver access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);

    await expect(listarContas()).rejects.toMatchObject({
      status: 401,
    });
  });

  test("propaga o Authorization: Bearer e devolve a lista", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => [{ id: "1", nome: "Conta X" }],
    });
    vi.stubGlobal("fetch", fetchMock);

    const contas = await listarContas();

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8081/api/v1/contas",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer token-abc",
        }),
      })
    );
    expect(contas).toEqual([{ id: "1", nome: "Conta X" }]);
  });

  test("usa a mensagem de erro do corpo da resposta quando falha", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ mensagem: "Campo inválido" }),
      })
    );

    await expect(listarContas()).rejects.toThrow("Campo inválido");
  });
});

describe("criarConta", () => {
  test("envia o corpo serializado em JSON", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "1", nome: "Conta X" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await criarConta({ nome: "Conta X", tipo: "CORRENTE" });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      nome: "Conta X",
      tipo: "CORRENTE",
    });
  });

  test("lança AccountServiceError com o status HTTP original", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Saldo insuficiente" }),
      })
    );

    await expect(criarConta({ nome: "X", tipo: "CORRENTE" })).rejects.toThrow(
      AccountServiceError
    );
  });
});
