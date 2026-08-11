"use server";

import {
  AiServiceError,
  definirConfiguracaoIa,
  enviarMensagem,
  type ChatResultado,
} from "@/lib/ai-service";

/**
 * Chamada direta do client component (não presa a <form>) — cada envio
 * de mensagem é uma chamada isolada, o estado da lista de mensagens
 * fica no client (services/web/app/(app)/chat/chat-client.tsx), não dá
 * pra modelar como useActionState (que espera um "estado" só, não uma
 * lista que cresce a cada chamada).
 */
export async function enviarMensagemAction(
  mensagem: string,
  conversaId?: string
): Promise<ChatResultado> {
  return enviarMensagem(mensagem, conversaId);
}

export interface ConfiguracaoFormState {
  erro?: string;
}

export async function definirConfiguracaoIaAction(
  _estadoAnterior: ConfiguracaoFormState,
  formData: FormData
): Promise<ConfiguracaoFormState> {
  const provedor = String(formData.get("provedor") ?? "");
  const apiKey = String(formData.get("apiKey") ?? "").trim();
  const ollamaUrl = String(formData.get("ollamaUrl") ?? "").trim();

  if (provedor !== "OPENAI" && provedor !== "OLLAMA") {
    return { erro: "Selecione um provedor" };
  }
  if (provedor === "OPENAI" && !apiKey) {
    return { erro: "Informe a API key da OpenAI" };
  }

  try {
    await definirConfiguracaoIa({
      provedor: provedor as "OPENAI" | "OLLAMA",
      apiKey: apiKey || undefined,
      ollamaUrl: ollamaUrl || undefined,
    });
  } catch (erro) {
    return {
      erro: erro instanceof AiServiceError ? erro.message : "Falha ao salvar configuração",
    };
  }

  return {};
}
