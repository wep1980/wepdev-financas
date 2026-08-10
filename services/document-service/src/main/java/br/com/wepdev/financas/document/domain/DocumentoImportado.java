package br.com.wepdev.financas.document.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root. Nunca cria transação sozinho — produz {@link LancamentoPendente}s
 * candidatos a partir do documento enviado, e só quando o usuário confirma
 * (via {@link #confirmar(Set)}) é que o caso de uso publica o evento Kafka
 * que o transaction-service consome (ver {@code overview.md} seção 3 e
 * ADR-0023).
 */
public class DocumentoImportado {

    private final UUID id;
    private final UUID usuarioId;
    private final TipoDocumento tipo;
    private final String nomeArquivo;
    private final byte[] conteudoArquivo;
    private StatusDocumento status;
    private String mensagemErro;
    private List<LancamentoPendente> lancamentos;
    private final Instant criadoEm;
    private Instant processadoEm;

    private DocumentoImportado(UUID id, UUID usuarioId, TipoDocumento tipo, String nomeArquivo, byte[] conteudoArquivo,
                                StatusDocumento status, String mensagemErro, List<LancamentoPendente> lancamentos,
                                Instant criadoEm, Instant processadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.nomeArquivo = nomeArquivo;
        this.conteudoArquivo = conteudoArquivo;
        this.status = status;
        this.mensagemErro = mensagemErro;
        this.lancamentos = new ArrayList<>(lancamentos);
        this.criadoEm = criadoEm;
        this.processadoEm = processadoEm;
    }

    /** Nasce RECEBIDO, sem lançamento nenhum — o caso de uso de upload processa e chama concluirComLancamentos/marcarErro em seguida. */
    public static DocumentoImportado receber(UUID usuarioId, TipoDocumento tipo, String nomeArquivo, byte[] conteudoArquivo) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(nomeArquivo, "nomeArquivo é obrigatório");
        Objects.requireNonNull(conteudoArquivo, "conteudoArquivo é obrigatório");
        if (nomeArquivo.isBlank()) {
            throw new IllegalArgumentException("nomeArquivo não pode ser vazio");
        }
        if (conteudoArquivo.length == 0) {
            throw new IllegalArgumentException("conteudoArquivo não pode ser vazio");
        }
        return new DocumentoImportado(UUID.randomUUID(), usuarioId, tipo, nomeArquivo, conteudoArquivo,
                StatusDocumento.RECEBIDO, null, List.of(), Instant.now(), null);
    }

    /** Reconstrói um documento já existente (vindo da persistência) — não valida como se fosse criação nova. */
    public static DocumentoImportado reconstituir(UUID id, UUID usuarioId, TipoDocumento tipo, String nomeArquivo,
                                                   byte[] conteudoArquivo, StatusDocumento status, String mensagemErro,
                                                   List<LancamentoPendente> lancamentos, Instant criadoEm,
                                                   Instant processadoEm) {
        return new DocumentoImportado(id, usuarioId, tipo, nomeArquivo, conteudoArquivo, status, mensagemErro,
                lancamentos, criadoEm, processadoEm);
    }

    /** Só sai de RECEBIDO — chamar de novo depois de já PROCESSANDO não faz nada. */
    public void iniciarProcessamento() {
        if (status == StatusDocumento.RECEBIDO) {
            status = StatusDocumento.PROCESSANDO;
        }
    }

    /** Extração terminou com pelo menos um lançamento reconhecido — fica aguardando o usuário revisar. */
    public void concluirComLancamentos(List<LancamentoPendente> lancamentosExtraidos) {
        Objects.requireNonNull(lancamentosExtraidos, "lancamentosExtraidos é obrigatório");
        if (lancamentosExtraidos.isEmpty()) {
            throw new IllegalArgumentException("lancamentosExtraidos não pode ser vazio — use marcarErro nesse caso");
        }
        this.lancamentos = new ArrayList<>(lancamentosExtraidos);
        this.status = StatusDocumento.AGUARDANDO_CONFIRMACAO;
        this.processadoEm = Instant.now();
    }

    /** PDF ilegível, ou extração rodou e não achou nenhum lançamento reconhecível. */
    public void marcarErro(String mensagem) {
        Objects.requireNonNull(mensagem, "mensagem é obrigatória");
        this.status = StatusDocumento.ERRO_PROCESSAMENTO;
        this.mensagemErro = mensagem;
        this.processadoEm = Instant.now();
    }

    /**
     * Confirma os lançamentos selecionados (viram CONFIRMADO), os demais
     * viram REJEITADO. Idempotente: chamar de novo um documento já
     * CONFIRMADO não faz nada (o caller decide se publica o evento Kafka de
     * novo — não publica, ver PagarFaturaUseCase do card-service pro mesmo
     * padrão de idempotência).
     */
    public void confirmar(Set<UUID> lancamentoIdsConfirmados) {
        Objects.requireNonNull(lancamentoIdsConfirmados, "lancamentoIdsConfirmados é obrigatório");
        if (status == StatusDocumento.CONFIRMADO) {
            return;
        }
        if (status != StatusDocumento.AGUARDANDO_CONFIRMACAO) {
            throw new DocumentoAindaNaoProcessadoException(id);
        }
        for (LancamentoPendente lancamento : lancamentos) {
            if (lancamentoIdsConfirmados.contains(lancamento.getId())) {
                lancamento.confirmar();
            } else {
                lancamento.rejeitar();
            }
        }
        status = StatusDocumento.CONFIRMADO;
    }

    public boolean isConfirmado() {
        return status == StatusDocumento.CONFIRMADO;
    }

    public List<LancamentoPendente> getLancamentosConfirmados() {
        return lancamentos.stream().filter(l -> l.getStatus() == StatusLancamento.CONFIRMADO).toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public byte[] getConteudoArquivo() {
        return conteudoArquivo;
    }

    public StatusDocumento getStatus() {
        return status;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public List<LancamentoPendente> getLancamentos() {
        return List.copyOf(lancamentos);
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
