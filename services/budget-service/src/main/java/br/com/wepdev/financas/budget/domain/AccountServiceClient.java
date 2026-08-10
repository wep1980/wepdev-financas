package br.com.wepdev.financas.budget.domain;

import java.util.List;

/**
 * Porta de saída pro account-service (chamada síncrona, ADR-0026),
 * propagando o token do próprio usuário — o endpoint já filtra pelo
 * `sub` do token, sem precisar de confirmação de posse (diferente do
 * padrão de card-service/document-service, que confirmam posse de um id
 * específico).
 */
public interface AccountServiceClient {

    /** Todas as contas ativas do usuário — filtrar por tipo (CORRENTE/CARTEIRA) é responsabilidade de quem chama. */
    List<Conta> buscarContasAtivas();
}
