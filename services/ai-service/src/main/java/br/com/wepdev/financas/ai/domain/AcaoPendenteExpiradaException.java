package br.com.wepdev.financas.ai.domain;

import java.time.Instant;
import java.util.UUID;

/** Proposta existia mas expirou (ADR-0007) — usuário precisa pedir a ação de novo, nunca confirma sobre contexto desatualizado. */
public class AcaoPendenteExpiradaException extends RuntimeException {

    public AcaoPendenteExpiradaException(UUID conversaId, Instant expirouEm) {
        super("Ação pendente da conversa " + conversaId + " expirou em " + expirouEm);
    }
}
