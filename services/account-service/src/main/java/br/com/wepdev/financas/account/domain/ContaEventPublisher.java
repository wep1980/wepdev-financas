package br.com.wepdev.financas.account.domain;

/**
 * Porta de saída pra eventos de domínio (tópico Kafka "conta.eventos",
 * ver docs/architecture/overview.md seção 7).
 */
public interface ContaEventPublisher {

    void publicarContaCriada(Conta conta);
}
