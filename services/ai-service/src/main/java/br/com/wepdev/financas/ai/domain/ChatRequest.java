package br.com.wepdev.financas.ai.domain;

import java.util.Objects;

/** {@code formatoJson}: pede ao modelo pra responder só com JSON — usado pelo agente orquestrador (item 8) pra extrair intent/parâmetros de forma confiável, mesmo recurso já usado no document-service. */
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
