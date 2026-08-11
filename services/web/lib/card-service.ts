import "server-only";
import { obterAccessToken } from "@/lib/auth-token";

export type Bandeira = "VISA" | "MASTERCARD" | "ELO" | "AMEX" | "OUTRA";

export interface Cartao {
  id: string;
  usuarioId: string;
  apelido: string;
  bandeira: Bandeira;
  limite: number;
  diaFechamento: number;
  diaVencimento: number;
  contaPagamentoId: string;
  ativo: boolean;
  criadoEm: string;
}

export interface CriarCartaoDados {
  apelido: string;
  bandeira: Bandeira;
  limite: number;
  diaFechamento: number;
  diaVencimento: number;
  contaPagamentoId: string;
}

export interface CompraResumo {
  compraId: string;
  cartaoId: string;
  descricao: string;
  categoria: string | null;
  valorParcela: number;
  quantidadeParcelas: number;
  parcelasRestantes: number;
  valorTotalRestante: number;
  finalizada: boolean;
}

export class CardServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.CARD_SERVICE_URL ?? "http://localhost:8083";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new CardServiceError("Sessão sem access token válido", 401);
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
    return corpo.mensagem ?? `Erro ${resposta.status} no card-service`;
  } catch {
    return `Erro ${resposta.status} no card-service`;
  }
}

export async function listarCartoes(): Promise<Cartao[]> {
  const resposta = await chamar("/api/v1/cartoes");
  if (!resposta.ok) {
    throw new CardServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}

export async function criarCartao(dados: CriarCartaoDados): Promise<Cartao> {
  const resposta = await chamar("/api/v1/cartoes", {
    method: "POST",
    body: JSON.stringify(dados),
  });
  if (!resposta.ok) {
    throw new CardServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}

export async function listarCompras(cartaoId: string): Promise<CompraResumo[]> {
  const resposta = await chamar(`/api/v1/cartoes/${cartaoId}/compras`);
  if (!resposta.ok) {
    throw new CardServiceError(await extrairErro(resposta), resposta.status);
  }
  return resposta.json();
}
