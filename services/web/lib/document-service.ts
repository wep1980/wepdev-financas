import "server-only";
import { obterAccessToken } from "@/lib/auth-token";

export type TipoDocumento = "FATURA_CARTAO";
export type StatusDocumento =
  | "RECEBIDO"
  | "PROCESSANDO"
  | "AGUARDANDO_CONFIRMACAO"
  | "CONFIRMADO"
  | "ERRO_PROCESSAMENTO";
export type StatusLancamento = "PENDENTE" | "CONFIRMADO" | "REJEITADO";
export type TipoLancamento = "RECEITA" | "DESPESA";

export interface LancamentoPendente {
  id: string;
  documentoId: string;
  descricao: string;
  valor: number;
  data: string;
  tipo: TipoLancamento;
  categoriaSugerida?: string | null;
  numeroParcela: number;
  quantidadeParcelas: number;
  status: StatusLancamento;
}

export interface DocumentoImportado {
  id: string;
  usuarioId: string;
  tipo: TipoDocumento;
  cartaoId: string;
  nomeArquivo: string;
  status: StatusDocumento;
  mensagemErro?: string | null;
  lancamentos: LancamentoPendente[];
  criadoEm: string;
  processadoEm?: string | null;
}

export class DocumentServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.DOCUMENT_SERVICE_URL ?? "http://localhost:8084";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new DocumentServiceError("Sessão sem access token válido", 401);
  }

  const headers = new Headers(init?.headers);
  headers.set("Authorization", `Bearer ${accessToken}`);
  // Content-Type NÃO é setado aqui de propósito: corpo JSON usa
  // application/json (setado por quem chama), corpo multipart (upload)
  // precisa do boundary que o próprio fetch calcula a partir do
  // FormData — setar Content-Type na mão quebraria o boundary.

  return fetch(`${baseUrl}${path}`, {
    ...init,
    headers,
    cache: "no-store",
  });
}

async function extrairErro(resposta: Response): Promise<string> {
  try {
    const corpo = await resposta.json();
    return corpo.mensagem ?? `Erro ${resposta.status} no document-service`;
  } catch {
    return `Erro ${resposta.status} no document-service`;
  }
}

async function verificarOk(resposta: Response): Promise<void> {
  if (!resposta.ok) {
    throw new DocumentServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function listarDocumentos(): Promise<DocumentoImportado[]> {
  const resposta = await chamar("/api/v1/documentos");
  await verificarOk(resposta);
  return resposta.json();
}

export async function buscarDocumento(id: string): Promise<DocumentoImportado> {
  const resposta = await chamar(`/api/v1/documentos/${id}`);
  await verificarOk(resposta);
  return resposta.json();
}

export async function uploadDocumento(
  arquivo: File,
  tipo: TipoDocumento,
  cartaoId: string,
  senha?: string
): Promise<DocumentoImportado> {
  const corpo = new FormData();
  corpo.set("arquivo", arquivo);
  corpo.set("tipo", tipo);
  corpo.set("cartaoId", cartaoId);
  if (senha) corpo.set("senha", senha);

  const resposta = await chamar("/api/v1/documentos", {
    method: "POST",
    body: corpo,
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function confirmarLancamentos(
  documentoId: string,
  lancamentoIdsConfirmados: string[]
): Promise<void> {
  const resposta = await chamar(`/api/v1/documentos/${documentoId}/confirmar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ lancamentoIdsConfirmados }),
  });
  await verificarOk(resposta);
}
