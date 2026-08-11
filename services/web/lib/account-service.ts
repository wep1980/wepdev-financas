import "server-only";
import { obterAccessToken } from "@/lib/auth-token";

export type TipoConta =
  | "CORRENTE"
  | "POUPANCA"
  | "CARTEIRA"
  | "CARTAO_CREDITO"
  | "INVESTIMENTO";

export interface Conta {
  id: string;
  usuarioId: string;
  nome: string;
  tipo: TipoConta;
  saldo: number;
  instituicao?: string;
  ativa: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface CriarContaDados {
  nome: string;
  tipo: TipoConta;
  saldoInicial?: number;
  instituicao?: string;
}

export interface AtualizarContaDados {
  nome: string;
  instituicao?: string;
}

export class AccountServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.ACCOUNT_SERVICE_URL ?? "http://localhost:8081";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new AccountServiceError("Sessão sem access token válido", 401);
  }

  return fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      ...init?.headers,
    },
    // Dado por usuário, nunca cacheado entre requests/usuários diferentes.
    cache: "no-store",
  });
}

async function extrairErro(resposta: Response): Promise<string> {
  try {
    const corpo = await resposta.json();
    return corpo.mensagem ?? `Erro ${resposta.status} no account-service`;
  } catch {
    return `Erro ${resposta.status} no account-service`;
  }
}

export async function listarContas(): Promise<Conta[]> {
  const resposta = await chamar("/api/v1/contas");
  if (!resposta.ok) {
    throw new AccountServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}

export async function criarConta(dados: CriarContaDados): Promise<Conta> {
  const resposta = await chamar("/api/v1/contas", {
    method: "POST",
    body: JSON.stringify(dados),
  });
  if (!resposta.ok) {
    throw new AccountServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}

export async function atualizarConta(
  id: string,
  dados: AtualizarContaDados
): Promise<Conta> {
  const resposta = await chamar(`/api/v1/contas/${id}`, {
    method: "PUT",
    body: JSON.stringify(dados),
  });
  if (!resposta.ok) {
    throw new AccountServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}

export async function excluirConta(id: string): Promise<void> {
  const resposta = await chamar(`/api/v1/contas/${id}`, { method: "DELETE" });
  if (!resposta.ok && resposta.status !== 404) {
    throw new AccountServiceError(await extrairErro(resposta), resposta.status);
  }
}
