package br.com.wepdev.financas.ai.application;

/** Uma tool MCP chamada pra montar a resposta — rastreabilidade (PRD seção 6: "a IA deveria conseguir explicar de onde tirou o número"). */
public record RegistroTrace(String nome, String resumo) {
}
