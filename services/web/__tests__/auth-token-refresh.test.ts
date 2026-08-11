import { describe, expect, test, vi } from "vitest";
import { precisaRenovar, renovarToken } from "@/lib/auth-token-refresh";

describe("precisaRenovar", () => {
  test("true quando já passou do expiresAt", () => {
    expect(precisaRenovar(1000, 1001)).toBe(true);
  });

  test("true quando é exatamente o expiresAt (sem margem)", () => {
    expect(precisaRenovar(1000, 1000)).toBe(true);
  });

  test("false quando ainda não expirou", () => {
    expect(precisaRenovar(1000, 999)).toBe(false);
  });

  test("true quando expiresAt é undefined (token nunca teve exchange)", () => {
    expect(precisaRenovar(undefined, 999)).toBe(true);
  });
});

describe("renovarToken", () => {
  test("troca o refresh_token e devolve o novo TokenSet", async () => {
    const agora = Math.floor(Date.now() / 1000);
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        access_token: "novo-access-token",
        refresh_token: "novo-refresh-token",
        id_token: "novo-id-token",
        expires_in: 300,
      }),
    });

    const resultado = await renovarToken(
      "refresh-token-antigo",
      "http://localhost:8080/realms/financas",
      "web-app",
      fetchMock as unknown as typeof fetch
    );

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/realms/financas/protocol/openid-connect/token",
      expect.objectContaining({ method: "POST" })
    );
    const body = fetchMock.mock.calls[0][1].body as URLSearchParams;
    expect(body.get("grant_type")).toBe("refresh_token");
    expect(body.get("client_id")).toBe("web-app");
    expect(body.get("refresh_token")).toBe("refresh-token-antigo");

    expect(resultado.accessToken).toBe("novo-access-token");
    expect(resultado.refreshToken).toBe("novo-refresh-token");
    expect(resultado.expiresAt).toBeGreaterThanOrEqual(agora + 300);
  });

  test("mantém o refresh_token antigo se o Keycloak não devolver um novo", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ access_token: "x", expires_in: 60 }),
    });

    const resultado = await renovarToken(
      "refresh-token-antigo",
      "http://localhost:8080/realms/financas",
      "web-app",
      fetchMock as unknown as typeof fetch
    );

    expect(resultado.refreshToken).toBe("refresh-token-antigo");
  });

  test("lança erro se a resposta do Keycloak não for ok", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue({ ok: false, status: 400, json: async () => ({}) });

    await expect(
      renovarToken(
        "refresh-token-invalido",
        "http://localhost:8080/realms/financas",
        "web-app",
        fetchMock as unknown as typeof fetch
      )
    ).rejects.toThrow(/Falha ao renovar/);
  });
});
