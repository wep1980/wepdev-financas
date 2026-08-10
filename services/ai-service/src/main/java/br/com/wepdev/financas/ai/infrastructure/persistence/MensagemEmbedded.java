package br.com.wepdev.financas.ai.infrastructure.persistence;

import java.time.Instant;

/** Documento embutido dentro de ConversaEntity — nunca é uma coleção própria (ver Mensagem no domínio). */
public class MensagemEmbedded {

    public String autor;

    public String texto;

    public String tipo;

    public Instant criadaEm;
}
