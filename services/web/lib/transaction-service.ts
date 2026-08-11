import "server-only";
import { obterAccessToken } from "@/lib/auth-token";

export type TipoTransacao = "RECEITA" | "DESPESA";
export type StatusTransacao = "PENDENTE" | "CONFIRMADA" | "CANCELADA";
export type FrequenciaRecorrencia = "MENSAL";
export type StatusRecorrente = "ATIVA" | "PAUSADA" | "CANCELADA" | "CONCLUIDA";

export interface Transacao {
  id: string;
  contaId: string;
  usuarioId: string;
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  categoria?: string;
  dataTransacao: string;
  status: StatusTransacao;
  transacaoRecorrenteId?: string | null;
  criadoEm: string;
}

export interface CriarTransacaoDados {
  contaId: string;
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  categoria?: string;
  dataTransacao?: string;
}

export interface AtualizarTransacaoDados {
  descricao: string;
  valor: number;
  categoria?: string;
  dataTransacao?: string;
}

export interface ResumoCategoria {
  categoria: string;
  totalGasto: number;
  percentualDoTotal: number;
  totalGastoPeriodoAnterior?: number | null;
}

export interface FiltroTransacoes {
  contaId?: string;
  inicio?: string;
  fim?: string;
}

export interface TransacaoRecorrente {
  id: string;
  contaId: string;
  usuarioId: string;
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  categoria?: string;
  frequencia: FrequenciaRecorrencia;
  dataInicio: string;
  quantidadeOcorrencias?: number | null;
  ocorrenciasGeradas: number;
  status: StatusRecorrente;
  criadoEm: string;
}

export interface CriarTransacaoRecorrenteDados {
  contaId: string;
  descricao: string;
  valor: number;
  tipo: TipoTransacao;
  categoria?: string;
  frequencia: FrequenciaRecorrencia;
  dataInicio: string;
  quantidadeOcorrencias?: number;
}

export class TransactionServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.TRANSACTION_SERVICE_URL ?? "http://localhost:8082";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new TransactionServiceError("Sessão sem access token válido", 401);
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
    return corpo.mensagem ?? `Erro ${resposta.status} no transaction-service`;
  } catch {
    return `Erro ${resposta.status} no transaction-service`;
  }
}

async function verificarOk(resposta: Response): Promise<void> {
  if (!resposta.ok) {
    throw new TransactionServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function listarTransacoes(
  filtro: FiltroTransacoes = {}
): Promise<Transacao[]> {
  const params = new URLSearchParams();
  if (filtro.contaId) params.set("contaId", filtro.contaId);
  if (filtro.inicio) params.set("inicio", filtro.inicio);
  if (filtro.fim) params.set("fim", filtro.fim);
  const query = params.toString();

  const resposta = await chamar(`/api/v1/transacoes${query ? `?${query}` : ""}`);
  await verificarOk(resposta);
  return resposta.json();
}

export async function resumoPorCategoria(
  inicio: string,
  fim: string
): Promise<ResumoCategoria[]> {
  const resposta = await chamar(
    `/api/v1/transacoes/resumo-por-categoria?inicio=${inicio}&fim=${fim}`
  );
  await verificarOk(resposta);
  return resposta.json();
}

export async function criarTransacao(dados: CriarTransacaoDados): Promise<Transacao> {
  const resposta = await chamar("/api/v1/transacoes", {
    method: "POST",
    body: JSON.stringify(dados),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function atualizarTransacao(
  id: string,
  dados: AtualizarTransacaoDados
): Promise<Transacao> {
  const resposta = await chamar(`/api/v1/transacoes/${id}`, {
    method: "PUT",
    body: JSON.stringify(dados),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function cancelarTransacao(id: string): Promise<void> {
  const resposta = await chamar(`/api/v1/transacoes/${id}`, { method: "DELETE" });
  if (!resposta.ok && resposta.status !== 404) {
    throw new TransactionServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function listarTransacoesRecorrentes(
  status?: StatusRecorrente
): Promise<TransacaoRecorrente[]> {
  const query = status ? `?status=${status}` : "";
  const resposta = await chamar(`/api/v1/transacoes-recorrentes${query}`);
  await verificarOk(resposta);
  return resposta.json();
}

export async function criarTransacaoRecorrente(
  dados: CriarTransacaoRecorrenteDados
): Promise<TransacaoRecorrente> {
  const resposta = await chamar("/api/v1/transacoes-recorrentes", {
    method: "POST",
    body: JSON.stringify(dados),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function cancelarTransacaoRecorrente(id: string): Promise<void> {
  const resposta = await chamar(`/api/v1/transacoes-recorrentes/${id}`, {
    method: "DELETE",
  });
  if (!resposta.ok && resposta.status !== 404) {
    throw new TransactionServiceError(await extrairErro(resposta), resposta.status);
  }
}
