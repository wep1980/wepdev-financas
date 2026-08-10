package br.com.wepdev.financas.ai.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. {@link Mensagem} e {@link AcaoPendente} vivem só dentro
 * de uma conversa (embutidos, nunca coleções próprias) — MongoDB modela
 * bem esse formato de "um documento por conversa", diferente do split em
 * duas tabelas usado por outros agregados deste sistema (ex.
 * DocumentoImportado/LancamentoPendente no document-service).
 */
public class Conversa {

    private final UUID id;
    private final UUID usuarioId;
    private final Instant iniciadaEm;
    private final List<Mensagem> mensagens;
    private AcaoPendente acaoPendente;

    private Conversa(UUID id, UUID usuarioId, Instant iniciadaEm, List<Mensagem> mensagens, AcaoPendente acaoPendente) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.iniciadaEm = iniciadaEm;
        this.mensagens = mensagens;
        this.acaoPendente = acaoPendente;
    }

    public static Conversa iniciar(UUID usuarioId) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        return new Conversa(UUID.randomUUID(), usuarioId, Instant.now(), new ArrayList<>(), null);
    }

    /** Reconstrói uma conversa já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Conversa reconstituir(UUID id, UUID usuarioId, Instant iniciadaEm, List<Mensagem> mensagens,
                                         AcaoPendente acaoPendente) {
        return new Conversa(id, usuarioId, iniciadaEm, new ArrayList<>(mensagens), acaoPendente);
    }

    public void adicionarMensagemUsuario(String texto) {
        mensagens.add(Mensagem.doUsuario(texto));
    }

    public void adicionarRespostaAgente(String texto, TipoRespostaAgente tipo) {
        mensagens.add(Mensagem.doAgente(texto, tipo));
    }

    /** Substitui qualquer proposta anterior — o agente só propõe uma ação por vez (correção reabre uma nova proposta). */
    public void proporAcao(AcaoPendente acao) {
        this.acaoPendente = Objects.requireNonNull(acao, "acao é obrigatória");
    }

    /** Devolve a ação pendente e limpa o estado — só chamado depois que o usuário confirma de fato. */
    public AcaoPendente confirmarAcaoPendente(Instant agora) {
        if (acaoPendente == null) {
            throw new NenhumaAcaoPendenteException(id);
        }
        if (acaoPendente.isExpirada(agora)) {
            Instant expirouEm = acaoPendente.getExpiraEm();
            acaoPendente = null;
            throw new AcaoPendenteExpiradaException(id, expirouEm);
        }
        AcaoPendente confirmada = acaoPendente;
        acaoPendente = null;
        return confirmada;
    }

    /** Descarta a proposta sem executar — usado quando o usuário muda de assunto em vez de confirmar/corrigir. */
    public void limparAcaoPendente() {
        acaoPendente = null;
    }

    public boolean temAcaoPendenteValida(Instant agora) {
        return acaoPendente != null && !acaoPendente.isExpirada(agora);
    }

    /** Última atividade da conversa — última mensagem, ou o início se ainda não tem nenhuma. Usado pra ordenar a listagem. */
    public Instant getUltimaAtividadeEm() {
        if (mensagens.isEmpty()) {
            return iniciadaEm;
        }
        return mensagens.get(mensagens.size() - 1).getCriadaEm();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public List<Mensagem> getMensagens() {
        return List.copyOf(mensagens);
    }

    public AcaoPendente getAcaoPendente() {
        return acaoPendente;
    }
}
