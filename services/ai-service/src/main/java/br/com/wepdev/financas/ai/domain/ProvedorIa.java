package br.com.wepdev.financas.ai.domain;

/** NENHUM = usuário nunca configurou (funcionalidade de IA fica desabilitada, ADR-0002). */
public enum ProvedorIa {
    NENHUM,
    OPENAI,
    OLLAMA
}
