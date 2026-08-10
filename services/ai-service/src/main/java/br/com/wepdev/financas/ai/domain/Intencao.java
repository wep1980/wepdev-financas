package br.com.wepdev.financas.ai.domain;

/** Classificação da mensagem do usuário (ai-strategy.md seção 4) — decidida pelo LLM, nunca por regra fixa de palavra-chave. */
public enum Intencao {
    CONSULTA,
    ACAO,
    DESCONHECIDA
}
