package br.com.wepdev.financas.ai.infrastructure.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Corpo de POST /v1/chat/completions — só os campos que este serviço usa, não o contrato inteiro da OpenAI. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatRequestDto(
        String model,
        List<OpenAiMensagemDto> messages,
        @JsonProperty("response_format") OpenAiResponseFormatDto responseFormat
) {
}
