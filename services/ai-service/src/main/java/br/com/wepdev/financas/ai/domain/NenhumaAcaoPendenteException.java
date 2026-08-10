package br.com.wepdev.financas.ai.domain;

import java.util.UUID;

/** Usuário tentou confirmar, mas não existe (ou já foi resolvida) nenhuma proposta de ação nessa conversa. */
public class NenhumaAcaoPendenteException extends RuntimeException {

    public NenhumaAcaoPendenteException(UUID conversaId) {
        super("Nenhuma ação pendente de confirmação na conversa: " + conversaId);
    }
}
