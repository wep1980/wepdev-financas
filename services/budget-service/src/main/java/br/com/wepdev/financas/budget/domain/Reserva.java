package br.com.wepdev.financas.budget.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Não é um aggregate no sentido tradicional — é uma configuração de valor
 * único por usuário (ADR-0026), sem histórico, sempre um upsert por
 * {@code usuarioId}.
 */
public class Reserva {

    private final UUID usuarioId;
    private BigDecimal valor;
    private Instant atualizadoEm;

    private Reserva(UUID usuarioId, BigDecimal valor, Instant atualizadoEm) {
        this.usuarioId = usuarioId;
        this.valor = valor;
        this.atualizadoEm = atualizadoEm;
    }

    public static Reserva definir(UUID usuarioId, BigDecimal valor) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        validarValor(valor);
        return new Reserva(usuarioId, valor, Instant.now());
    }

    /** Reconstrói uma reserva já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Reserva reconstituir(UUID usuarioId, BigDecimal valor, Instant atualizadoEm) {
        return new Reserva(usuarioId, valor, atualizadoEm);
    }

    /** Estado default de quem nunca definiu uma reserva — atualizadoEm nulo sinaliza isso na resposta. */
    public static Reserva semDefinir(UUID usuarioId) {
        return new Reserva(usuarioId, BigDecimal.ZERO, null);
    }

    public void atualizar(BigDecimal novoValor) {
        validarValor(novoValor);
        this.valor = novoValor;
        this.atualizadoEm = Instant.now();
    }

    private static void validarValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("valor não pode ser negativo");
        }
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
