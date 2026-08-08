package br.com.wepdev.financas.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do domínio de transações. Quando uma transação é criada
 * (via {@link #criar}), o efeito no saldo da conta já aconteceu (o caso de
 * uso chama o account-service antes de persistir) — por isso nasce
 * CONFIRMADA, nunca PENDENTE.
 */
public class Transacao {

    private final UUID id;
    private final UUID contaId;
    private final UUID usuarioId;
    private String descricao;
    private BigDecimal valor;
    private final TipoTransacao tipo;
    private String categoria;
    private LocalDate dataTransacao;
    private StatusTransacao status;
    private final UUID transacaoRecorrenteId;
    private final Instant criadoEm;

    private Transacao(UUID id, UUID contaId, UUID usuarioId, String descricao, BigDecimal valor, TipoTransacao tipo,
                       String categoria, LocalDate dataTransacao, StatusTransacao status,
                       UUID transacaoRecorrenteId, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.usuarioId = usuarioId;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.categoria = categoria;
        this.dataTransacao = dataTransacao;
        this.status = status;
        this.transacaoRecorrenteId = transacaoRecorrenteId;
        this.criadoEm = criadoEm;
    }

    public static Transacao criar(UUID contaId, UUID usuarioId, String descricao, BigDecimal valor, TipoTransacao tipo,
                                   String categoria, LocalDate dataTransacao) {
        return criar(contaId, usuarioId, descricao, valor, tipo, categoria, dataTransacao, null);
    }

    /** transacaoRecorrenteId preenchido quando essa ocorrência foi gerada por uma TransacaoRecorrente (ver /transacoes-recorrentes). */
    public static Transacao criar(UUID contaId, UUID usuarioId, String descricao, BigDecimal valor, TipoTransacao tipo,
                                   String categoria, LocalDate dataTransacao, UUID transacaoRecorrenteId) {
        Objects.requireNonNull(contaId, "contaId é obrigatório");
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
        LocalDate data = dataTransacao == null ? LocalDate.now() : dataTransacao;
        return new Transacao(UUID.randomUUID(), contaId, usuarioId, descricao, valor, tipo, categoria, data,
                StatusTransacao.CONFIRMADA, transacaoRecorrenteId, Instant.now());
    }

    /** Reconstrói uma transação já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Transacao reconstituir(UUID id, UUID contaId, UUID usuarioId, String descricao, BigDecimal valor,
                                          TipoTransacao tipo, String categoria, LocalDate dataTransacao,
                                          StatusTransacao status, UUID transacaoRecorrenteId, Instant criadoEm) {
        return new Transacao(id, contaId, usuarioId, descricao, valor, tipo, categoria, dataTransacao, status,
                transacaoRecorrenteId, criadoEm);
    }

    /** contaId, tipo e usuarioId não são editáveis — trocar de conta/tipo é cancelar e recriar (evita ambiguidade de reversão entre contas diferentes). */
    public void atualizar(String descricao, BigDecimal valor, String categoria, LocalDate dataTransacao) {
        Objects.requireNonNull(descricao, "descricao é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
        this.descricao = descricao;
        this.valor = valor;
        this.categoria = categoria;
        if (dataTransacao != null) {
            this.dataTransacao = dataTransacao;
        }
    }

    /** Idempotente — cancelar de novo uma transação já cancelada é um no-op, o chamador decide se reverte saldo. */
    public void cancelar() {
        status = StatusTransacao.CANCELADA;
    }

    public boolean isCancelada() {
        return status == StatusTransacao.CANCELADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public LocalDate getDataTransacao() {
        return dataTransacao;
    }

    public StatusTransacao getStatus() {
        return status;
    }

    public UUID getTransacaoRecorrenteId() {
        return transacaoRecorrenteId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
