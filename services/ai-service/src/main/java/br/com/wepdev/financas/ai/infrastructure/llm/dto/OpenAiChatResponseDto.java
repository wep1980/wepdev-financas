package br.com.wepdev.financas.ai.infrastructure.llm.dto;

import java.util.List;

/** Resposta de POST /v1/chat/completions — só os campos que este serviço usa. */
public record OpenAiChatResponseDto(List<OpenAiEscolhaDto> choices) {
}
