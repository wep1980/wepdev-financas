package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Um orçamento é sempre categoria + mês (ver ADR-0026) —
 * {@code valorConsumido}/{@code valorDisponivel} não fazem parte do
 * domínio: são calculados na hora pelo caso de uso, consultando o
 * transaction-service, nunca persistidos aqui.
 */
public class Orcamento {

    private final UUID id;
    private final UUID usuarioId;
    private final String categoria;
    private final YearMonth mesReferencia;
    private BigDecimal valorLimite;
    private StatusOrcamento status;
    private final Instant criadoEm;

    private Orcamento(UUID id, UUID usuarioId, String categoria, YearMonth mesReferencia, BigDecimal valorLimite,
                       StatusOrcamento status, Instant criadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.categoria = categoria;
        this.mesReferencia = mesReferencia;
        this.valorLimite = valorLimite;
        this.status = status;
        this.criadoEm = criadoEm;
    }

    public static Orcamento criar(UUID usuarioId, String categoria, YearMonth mesReferencia, BigDecimal valorLimite) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(mesReferencia, "mesReferencia é obrigatório");
        validarCategoria(categoria);
        validarValorLimite(valorLimite);
        return new Orcamento(UUID.randomUUID(), usuarioId, categoria, mesReferencia, valorLimite,
                StatusOrcamento.ATIVO, Instant.now());
    }

    /** Reconstrói um orçamento já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Orcamento reconstituir(UUID id, UUID usuarioId, String categoria, YearMonth mesReferencia,
                                          BigDecimal valorLimite, StatusOrcamento status, Instant criadoEm) {
        return new Orcamento(id, usuarioId, categoria, mesReferencia, valorLimite, status, criadoEm);
    }

    public void atualizarLimite(BigDecimal novoValorLimite) {
        validarValorLimite(novoValorLimite);
        this.valorLimite = novoValorLimite;
    }

    /** Idempotente — cancelar de novo um orçamento já cancelado não faz nada. */
    public void cancelar() {
        status = StatusOrcamento.CANCELADO;
    }

    private static void validarCategoria(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            throw new IllegalArgumentException("categoria não pode ser vazia");
        }
    }

    private static void validarValorLimite(BigDecimal valorLimite) {
        Objects.requireNonNull(valorLimite, "valorLimite é obrigatório");
        if (valorLimite.signum() <= 0) {
            throw new IllegalArgumentException("valorLimite precisa ser positivo");
        }
    }

    public boolean isAtivo() {
        return status == StatusOrcamento.ATIVO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getCategoria() {
        return categoria;
    }

    public YearMonth getMesReferencia() {
        return mesReferencia;
    }

    public BigDecimal getValorLimite() {
        return valorLimite;
    }

    public StatusOrcamento getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
