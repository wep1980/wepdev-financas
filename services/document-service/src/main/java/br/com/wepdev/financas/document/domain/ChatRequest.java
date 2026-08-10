package br.com.wepdev.financas.document.domain;

import java.util.Objects;

/**
 * {@code formatoJson}: pede ao modelo pra responder só com JSON (suportado
 * pelo Ollama via "format": "json" — melhora muito a confiabilidade do
 * parsing do agente de extração, ver AgenteExtracaoFaturaService).
 * Anexo de imagem opcional (ADR-0015, ingestão por foto) entra aqui quando
 * a fatia de foto for implementada — não adiantar agora (só texto extraído
 * de PDF nessa fatia).
 */
public record ChatRequest(String prompt, boolean formatoJson) {

    public ChatRequest {
        Objects.requireNonNull(prompt, "prompt é obrigatório");
    }

    public static ChatRequest deTexto(String prompt) {
        return new ChatRequest(prompt, false);
    }

    public static ChatRequest pedindoJson(String prompt) {
        return new ChatRequest(prompt, true);
    }
}
