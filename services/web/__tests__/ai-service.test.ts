import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  AiServiceError,
  enviarMensagem,
  definirConfiguracaoIa,
} from "@/lib/ai-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("enviarMensagem", () => {
  test("lança 401 sem access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);
    await expect(enviarMensagem("oi")).rejects.toMatchObject({ status: 401 });
  });

  test("envia conversaId null quando é uma conversa nova", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ conversaId: "c1", resposta: "oi!", tipo: "RESPOSTA", trace: [] }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await enviarMensagem("oi");

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(init.body)).toEqual({ mensagem: "oi", conversaId: null });
  });

  test("propaga o conversaId quando informado", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ conversaId: "c1", resposta: "ok", tipo: "RESPOSTA", trace: [] }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await enviarMensagem("sim", "c1");

    const [, init] = fetchMock.mock.calls[0];
    expect(JSON.parse(init.body)).toEqual({ mensagem: "sim", conversaId: "c1" });
  });

  test("lança 422 quando o usuário não tem provedor de IA configurado", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Provedor de IA não configurado" }),
      })
    );

    await expect(enviarMensagem("oi")).rejects.toThrow(AiServiceError);
  });
});

describe("definirConfiguracaoIa", () => {
  test("envia o provedor e a apiKey em JSON", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ provedor: "OPENAI", configurado: true }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await definirConfiguracaoIa({ provedor: "OPENAI", apiKey: "sk-teste" });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8086/api/v1/configuracao");
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body)).toEqual({
      provedor: "OPENAI",
      apiKey: "sk-teste",
    });
  });
});
