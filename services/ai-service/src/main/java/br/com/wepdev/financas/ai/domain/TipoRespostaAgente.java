package br.com.wepdev.financas.ai.domain;

/** RESPOSTA = consulta respondida; PROPOSTA_ACAO = aguardando confirmação (ADR-0007); ACAO_EXECUTADA = mutação já aconteceu. */
public enum TipoRespostaAgente {
    RESPOSTA,
    PROPOSTA_ACAO,
    ACAO_EXECUTADA
}
