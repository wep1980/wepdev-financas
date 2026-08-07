package br.com.wepdev.financas.transaction.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Porta de saída pro account-service (chamada síncrona, ADR-0003/ADR-0009).
 * A implementação (infrastructure) é responsável por: (1) confirmar que a
 * conta pertence ao usuário autenticado — reusando o mesmo 404 do
 * account-service pra conta inexistente/de outro usuário — e só então (2)
 * aplicar o ajuste via endpoint interno (role service).
 */
public interface AccountServiceClient {

    void debitar(UUID contaId, BigDecimal valor);

    void creditar(UUID contaId, BigDecimal valor);
}
