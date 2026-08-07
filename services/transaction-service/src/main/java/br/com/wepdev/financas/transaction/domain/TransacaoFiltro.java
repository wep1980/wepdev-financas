package br.com.wepdev.financas.transaction.domain;

import java.time.LocalDate;
import java.util.UUID;

/** contaId/inicio/fim são opcionais — null significa "sem esse filtro". usuarioId é sempre obrigatório (nunca lista transação de outro usuário). */
public record TransacaoFiltro(
        UUID usuarioId,
        UUID contaId,
        LocalDate inicio,
        LocalDate fim
) {
    public TransacaoFiltro {
        if (usuarioId == null) {
            throw new NullPointerException("usuarioId é obrigatório");
        }
    }
}
