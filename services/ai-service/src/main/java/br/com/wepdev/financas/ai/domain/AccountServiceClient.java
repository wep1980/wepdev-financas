package br.com.wepdev.financas.ai.domain;

import java.util.List;

/**
 * Porta de saída pro account-service, propagando o token do próprio
 * usuário — usada só pelo agente orquestrador (item 8) pra resolver o
 * texto livre de conta mencionado num comando de ação ("conta corrente",
 * "carteira") pro id de verdade, antes de propor a ação (ADR-0007). Não
 * é usada por nenhuma tool de consulta (item 6 já não precisava dela —
 * budget-service agrega saldo internamente).
 */
public interface AccountServiceClient {

    List<Conta> buscarContasAtivas();
}
