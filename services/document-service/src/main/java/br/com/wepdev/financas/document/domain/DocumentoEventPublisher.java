package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/**
 * Porta de saída (Dependency Inversion) — publica o evento
 * "documento.lancamentos-confirmados" (Kafka) que o transaction-service
 * consome pra criar as transações de verdade (débito/crédito síncrono com
 * account-service), ver docs/architecture/overview.md seção 3 e ADR-0023.
 */
public interface DocumentoEventPublisher {

    /**
     * {@code contaId}: conta que vai debitar/creditar os lançamentos
     * confirmados — o document-service não sabe disso sozinho (ADR-0023),
     * o usuário escolhe no momento da confirmação (ver
     * ConfirmarLancamentosUseCase). Publica só os lançamentos
     * {@link StatusLancamento#CONFIRMADO} de {@code documento}.
     */
    void publicarLancamentosConfirmados(DocumentoImportado documento, UUID contaId);
}
