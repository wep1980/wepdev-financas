package br.com.wepdev.financas.document.domain;

import java.util.UUID;

/**
 * Porta de saída pro account-service (ADR-0025) — só confirma posse de
 * conta antes de publicar o evento de confirmação (ver
 * ConfirmarLancamentosUseCase); débito/crédito de verdade acontece no
 * transaction-service, que consome o evento. Não é integração com
 * card-service (isso continua fora de escopo, ADR-0023).
 */
public interface AccountServiceClient {

    /** @throws ContaNaoEncontradaException se a conta não existir ou não pertencer ao usuário autenticado. */
    void confirmarPosseDaConta(UUID contaId);
}
