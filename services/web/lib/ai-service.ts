import "server-only";
import { cache } from "react";
import { obterAccessToken } from "@/lib/auth-token";

export type TipoRespostaAgente = "RESPOSTA" | "PROPOSTA_ACAO" | "ACAO_EXECUTADA";
export type TipoAcaoProposta = "RECEITA" | "DESPESA";
export type AutorMensagem = "USUARIO" | "AGENTE";
export type ProvedorIa = "NENHUM" | "OPENAI" | "OLLAMA";

export interface AcaoProposta {
  tipo: TipoAcaoProposta;
  descricao: string;
  valor: number;
  recorrente: boolean;
  frequencia?: "MENSAL" | null;
  quantidadeOcorrencias?: number | null;
  contaId?: string | null;
  categoria?: string | null;
  expiraEm: string;
}

export interface ToolInvocada {
  nome: string;
  resumo: string;
}

export interface ChatResultado {
  conversaId: string;
  resposta: string;
  tipo: TipoRespostaAgente;
  acaoProposta?: AcaoProposta | null;
  trace: ToolInvocada[];
}

export interface ConversaResumo {
  id: string;
  iniciadaEm: string;
  ultimaMensagemEm: string;
  ultimaMensagemPreview: string;
}

export interface MensagemConversa {
  autor: AutorMensagem;
  texto: string;
  tipo?: TipoRespostaAgente | null;
  criadaEm: string;
}

export interface ConversaDetalhe {
  id: string;
  iniciadaEm: string;
  mensagens: MensagemConversa[];
}

export interface ConfiguracaoIa {
  provedor: ProvedorIa;
  configurado: boolean;
  ollamaUrl?: string | null;
}

export interface DefinirConfiguracaoIaDados {
  provedor: "OPENAI" | "OLLAMA";
  apiKey?: string;
  ollamaUrl?: string;
}

export class AiServiceError extends Error {
  constructor(
    message: string,
    public readonly status: number
  ) {
    super(message);
  }
}

const baseUrl = process.env.AI_SERVICE_URL ?? "http://localhost:8086";

async function chamar(path: string, init?: RequestInit): Promise<Response> {
  const accessToken = await obterAccessToken();
  if (!accessToken) {
    throw new AiServiceError("Sessão sem access token válido", 401);
  }

  return fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
      ...init?.headers,
    },
    // Conversa é por usuário, nunca cacheada entre requests/usuários.
    cache: "no-store",
  });
}

async function extrairErro(resposta: Response): Promise<string> {
  try {
    const corpo = await resposta.json();
    return corpo.mensagem ?? `Erro ${resposta.status} no ai-service`;
  } catch {
    return `Erro ${resposta.status} no ai-service`;
  }
}

async function verificarOk(resposta: Response): Promise<void> {
  if (!resposta.ok) {
    throw new AiServiceError(await extrairErro(resposta), resposta.status);
  }
}

export async function enviarMensagem(
  mensagem: string,
  conversaId?: string
): Promise<ChatResultado> {
  const resposta = await chamar("/api/v1/chat", {
    method: "POST",
    body: JSON.stringify({ mensagem, conversaId: conversaId ?? null }),
  });
  await verificarOk(resposta);
  return resposta.json();
}

export async function listarConversas(): Promise<ConversaResumo[]> {
  const resposta = await chamar("/api/v1/conversas");
  await verificarOk(resposta);
  return resposta.json();
}

export async function buscarConversa(id: string): Promise<ConversaDetalhe> {
  const resposta = await chamar(`/api/v1/conversas/${id}`);
  await verificarOk(resposta);
  return resposta.json();
}

// cache() do React — dedupe por request: layout do chat e a página
// (nova conversa ou conversa existente) chamam isso separadamente na
// mesma renderização, sem isso seriam duas chamadas HTTP idênticas ao
// ai-service (mesmo princípio já usado em lib/auth-token.ts).
export const buscarConfiguracaoIa = cache(async (): Promise<ConfiguracaoIa> => {
  const resposta = await chamar("/api/v1/configuracao");
  await verificarOk(resposta);
  return resposta.json();
});

export async function definirConfiguracaoIa(
  dados: DefinirConfiguracaoIaDados
): Promise<ConfiguracaoIa> {
  const resposta = await chamar("/api/v1/configuracao", {
    method: "PUT",
    body: JSON.stringify(dados),
  });
  await verificarOk(resposta);
  return resposta.json();
}
