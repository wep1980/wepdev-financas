import "server-only";
import { obterAccessToken } from "@/lib/auth-token";

export type StatusOrcamento = "ATIVO" | "CANCELADO";

export interface Orcamento {
  id: string;
  usuarioId: string;
  categoria: string;
  mesReferencia: string;
  valorLimite: number;
  valorConsumido: number;
  valorDisponivel: number;
  percentualConsumido: number;
  status: StatusOrcamento;
  criadoEm: string;
}

export interface CriarOrcamentoDados {
  categoria: string;
  mesReferencia: string;
  valorLimite: number;
}

export interface Reserva {
  usuarioId: string;
  valor: number;
  atualizadoEm: string | null;
}

export interface ContaResumo {
  contaId: string;
  nome: string;
  tipo: string;
  saldo: number;
}

export interface FaturaResumo {
  faturaId: string;
  cartaoApelido: string;
  valorTotal: number;
  dataVencimento: string;
}

export interface DespesaRecorrenteResumo {
  transacaoRecorrenteId: string;
  descricao: string;
  valor: number;
}

export interface DisponivelParaGastar {
  mesReferencia: string;
  saldoContas: number;
  faturasEmAberto: number;
  despesasRecorrentes: number;
  reserva: number;
  valorDisponivel: number;
  detalhamento: {
    contas: ContaResumo[];
    faturas: FaturaResumo[];
    despesasRecorrentes: DespesaRecorrenteResumo[];
  };
}

export class BudgetServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.BUDGET_SERVICE_URL ?? "http://localhost:8085";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new BudgetServiceError("Sessão sem access token válido", 401);
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
    return corpo.mensagem ?? `Erro ${resposta.status} no budget-service`;
  } catch {
    return `Erro ${resposta.status} no budget-service`;
  }
}

async function verificarOk(resposta: Response): Promise<void> {
  if (!resposta.ok) {
    throw new BudgetServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function listarOrcamentos(mes: string): Promise<Orcamento[]> {
  const resposta = await chamar(`/api/v1/orcamentos?mes=${mes}`);
  await verificarOk(resposta);
  return resposta.json();
}

export async function criarOrcamento(dados: CriarOrcamentoDados): Promise<Orcamento> {
  const resposta = await chamar("/api/v1/orcamentos", {
    method: "POST",
    body: JSON.stringify(dados),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function atualizarOrcamento(
  id: string,
  valorLimite: number
): Promise<Orcamento> {
  const resposta = await chamar(`/api/v1/orcamentos/${id}`, {
    method: "PUT",
    body: JSON.stringify({ valorLimite }),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function cancelarOrcamento(id: string): Promise<void> {
  const resposta = await chamar(`/api/v1/orcamentos/${id}`, { method: "DELETE" });
  if (!resposta.ok && resposta.status !== 404) {
    throw new BudgetServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function buscarReserva(): Promise<Reserva> {
  const resposta = await chamar("/api/v1/reserva");
  await verificarOk(resposta);
  return resposta.json();
}

export async function definirReserva(valor: number): Promise<Reserva> {
  const resposta = await chamar("/api/v1/reserva", {
    method: "PUT",
    body: JSON.stringify({ valor }),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function buscarDisponivelParaGastar(
  mes: string
): Promise<DisponivelParaGastar> {
  const resposta = await chamar(`/api/v1/disponivel-para-gastar?mes=${mes}`);
  await verificarOk(resposta);
  return resposta.json();
}
