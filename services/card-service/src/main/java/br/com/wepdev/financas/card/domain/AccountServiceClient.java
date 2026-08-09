package br.com.wepdev.financas.card.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Porta de saída pro account-service (chamada síncrona, ADR-0003/ADR-0022).
 * A implementação confirma que a conta pertence ao usuário autenticado,
 * reusando o mesmo 404 do account-service pra conta inexistente/de outro
 * usuário — nunca reimplementa essa checagem aqui.
 */
public interface AccountServiceClient {

    void confirmarPosseDaConta(UUID contaId);

    /** Chamado só ao pagar fatura (POST /faturas/{id}/pagar) — confirma posse de novo antes de debitar. */
    void debitar(UUID contaId, BigDecimal valor);
}
