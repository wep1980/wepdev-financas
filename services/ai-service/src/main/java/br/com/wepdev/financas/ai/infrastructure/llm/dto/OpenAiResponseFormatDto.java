package br.com.wepdev.financas.ai.infrastructure.llm.dto;

public record OpenAiResponseFormatDto(String type) {

    public static OpenAiResponseFormatDto json() {
        return new OpenAiResponseFormatDto("json_object");
    }
}
