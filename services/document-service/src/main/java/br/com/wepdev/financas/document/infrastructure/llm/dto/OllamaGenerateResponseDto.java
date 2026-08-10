package br.com.wepdev.financas.document.infrastructure.llm.dto;

/** Só o campo "response" (texto gerado) — Ollama devolve bem mais coisa (métricas de tempo etc.), sem uso aqui. */
public record OllamaGenerateResponseDto(String response) {
}
