package br.com.wepdev.financas.budget.domain;

import java.util.List;

/**
 * Porta de saída pro card-service (chamada síncrona, ADR-0026), propagando
 * o token do próprio usuário — sem confirmação de posse, os endpoints já
 * filtram pelo `sub` do token.
 */
public interface CardServiceClient {

    /**
     * Todas as faturas FECHADA (não paga, valor já definitivo — ver
     * ADR-0026) de todos os cartões ativos do usuário, de qualquer mês.
     * Filtrar por dataVencimento dentro do mês consultado é
     * responsabilidade de quem chama.
     */
    List<FaturaFechada> buscarFaturasFechadas();
}
