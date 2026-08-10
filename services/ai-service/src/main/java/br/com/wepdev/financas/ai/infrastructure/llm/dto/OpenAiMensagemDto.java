package br.com.wepdev.financas.ai.infrastructure.llm.dto;

/** Usado tanto no request (role="user") quanto na resposta (role="assistant") — mesmo formato dos dois lados na API da OpenAI. */
public record OpenAiMensagemDto(String role, String content) {
}
