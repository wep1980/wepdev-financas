"use server";

import {
  AiServiceError,
  definirConfiguracaoIa,
  enviarMensagem,
  type ChatResultado,
} from "@/lib/ai-service";

export type EnviarMensagemResultado =
  | { sucesso: true; resultado: ChatResultado }
  | { sucesso: false; erro: string };

/**
 * Chamada direta do client component (não presa a <form>) — cada envio
 * de mensagem é uma chamada isolada, o estado da lista de mensagens
 * fica no client (services/web/app/(app)/chat/chat-client.tsx), não dá
 * pra modelar como useActionState (que espera um "estado" só, não uma
 * lista que cresce a cada chamada).
 *
 * Sempre RETORNA o erro, nunca deixa propagar (throw) — achado real
 * (2026-08-11, testando com o Ollama sob carga/timeout de verdade):
 * uma Server Action que lança exceção tem a mensagem REDACTADA pelo
 * Next.js em produção (troca por um "Minified React error #441"
 * genérico, sem nenhuma pista pro usuário), mesmo com try/catch do lado
 * do client — o catch pega o erro, mas `.message` já vem sanitizado
 * pelo framework antes de chegar lá. Único jeito de preservar a
 * mensagem real (do `AiServiceError`, com o texto que o `ai-service`
 * devolveu) é nunca lançar através da fronteira da Server Action —
 * mesmo padrão que `definirConfiguracaoIaAction` logo abaixo já usava,
 * só não tinha sido aplicado aqui.
 */
export async function enviarMensagemAction(
  mensagem: string,
  conversaId?: string
): Promise<EnviarMensagemResultado> {
  try {
    const resultado = await enviarMensagem(mensagem, conversaId);
    return { sucesso: true, resultado };
  } catch (erro) {
    return {
      sucesso: false,
      erro:
        erro instanceof AiServiceError
          ? erro.message
          : "Falha ao enviar mensagem",
    };
  }
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
