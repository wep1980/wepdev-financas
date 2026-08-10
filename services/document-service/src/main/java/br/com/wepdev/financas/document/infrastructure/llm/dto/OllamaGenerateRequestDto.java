package br.com.wepdev.financas.document.infrastructure.llm.dto;

/** Corpo de POST /api/generate — só os campos que este serviço usa, não o contrato inteiro do Ollama. */
public record OllamaGenerateRequestDto(String model, String prompt, boolean stream, String format, OllamaOptionsDto options) {
}
