import { afterEach, describe, expect, test, vi } from "vitest";

vi.mock("@/lib/auth-token", () => ({
  obterAccessToken: vi.fn(),
}));

import { obterAccessToken } from "@/lib/auth-token";
import {
  DocumentServiceError,
  confirmarLancamentos,
  listarDocumentos,
  uploadDocumento,
} from "@/lib/document-service";

const obterAccessTokenMock = vi.mocked(obterAccessToken);

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("listarDocumentos", () => {
  test("lança 401 sem access token", async () => {
    obterAccessTokenMock.mockResolvedValue(null);
    await expect(listarDocumentos()).rejects.toMatchObject({ status: 401 });
  });
});

describe("uploadDocumento", () => {
  test("envia multipart/form-data sem fixar Content-Type na mão", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ id: "doc-1", status: "RECEBIDO" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const arquivo = new File(["conteudo"], "fatura.pdf", { type: "application/pdf" });
    await uploadDocumento(arquivo, "FATURA_CARTAO", "cartao-1", "senha-do-pdf");

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8084/api/v1/documentos");
    expect(init.body).toBeInstanceOf(FormData);
    expect((init.body as FormData).get("tipo")).toBe("FATURA_CARTAO");
    expect((init.body as FormData).get("cartaoId")).toBe("cartao-1");
    expect((init.body as FormData).get("senha")).toBe("senha-do-pdf");
    // Content-Type não pode ser setado na mão — o fetch calcula o
    // boundary do multipart sozinho a partir do FormData.
    const headers = init.headers as Headers;
    expect(headers.has("content-type")).toBe(false);
    expect(headers.get("authorization")).toBe("Bearer token-abc");
  });

  test("lança DocumentServiceError 400 se o tipo for inválido", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 400,
        json: async () => ({ mensagem: "Arquivo ausente ou tipo inválido" }),
      })
    );

    const arquivo = new File(["x"], "fatura.pdf");
    await expect(uploadDocumento(arquivo, "FATURA_CARTAO", "cartao-1")).rejects.toThrow(
      DocumentServiceError
    );
  });
});

describe("confirmarLancamentos", () => {
  test("envia lancamentoIdsConfirmados em JSON", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, json: async () => ({}) });
    vi.stubGlobal("fetch", fetchMock);

    await confirmarLancamentos("doc-1", ["l1", "l2"]);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe("http://localhost:8084/api/v1/documentos/doc-1/confirmar");
    expect(JSON.parse(init.body)).toEqual({
      lancamentoIdsConfirmados: ["l1", "l2"],
    });
  });

  test("lança 422 quando documento ainda não terminou de processar", async () => {
    obterAccessTokenMock.mockResolvedValue("token-abc");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({ mensagem: "Documento ainda não terminou de processar" }),
      })
    );

    await expect(confirmarLancamentos("doc-1", ["l1"])).rejects.toThrow(
      DocumentServiceError
    );
  });
});
