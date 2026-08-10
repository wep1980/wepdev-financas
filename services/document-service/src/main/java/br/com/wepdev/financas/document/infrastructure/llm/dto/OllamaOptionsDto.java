package br.com.wepdev.financas.document.infrastructure.llm.dto;

/** Temperatura baixa pra tarefa de extração estruturada (menos variação entre chamadas) — testado na prática (2026-08-09). */
public record OllamaOptionsDto(double temperature) {
}
