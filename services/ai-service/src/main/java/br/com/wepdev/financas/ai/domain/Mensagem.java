package br.com.wepdev.financas.ai.domain;

import java.time.Instant;
import java.util.Objects;

/** Value object, sempre embutido numa {@link Conversa} — nunca existe fora dela. */
public final class Mensagem {

    private final AutorMensagem autor;
    private final String texto;
    private final TipoRespostaAgente tipo;
    private final Instant criadaEm;

    private Mensagem(AutorMensagem autor, String texto, TipoRespostaAgente tipo, Instant criadaEm) {
        this.autor = autor;
        this.texto = texto;
        this.tipo = tipo;
        this.criadaEm = criadaEm;
    }

    public static Mensagem doUsuario(String texto) {
        validarTexto(texto);
        return new Mensagem(AutorMensagem.USUARIO, texto, null, Instant.now());
    }

    public static Mensagem doAgente(String texto, TipoRespostaAgente tipo) {
        validarTexto(texto);
        Objects.requireNonNull(tipo, "tipo é obrigatório pra mensagem do agente");
        return new Mensagem(AutorMensagem.AGENTE, texto, tipo, Instant.now());
    }

    /** Reconstrói uma mensagem já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Mensagem reconstituir(AutorMensagem autor, String texto, TipoRespostaAgente tipo, Instant criadaEm) {
        return new Mensagem(autor, texto, tipo, criadaEm);
    }

    private static void validarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("texto não pode ser vazio");
        }
    }

    public AutorMensagem getAutor() {
        return autor;
    }

    public String getTexto() {
        return texto;
    }

    public TipoRespostaAgente getTipo() {
        return tipo;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}
