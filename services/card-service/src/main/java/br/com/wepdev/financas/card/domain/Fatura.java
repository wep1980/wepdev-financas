package br.com.wepdev.financas.card.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Nasce ABERTA e vazia (valorTotal = 0) — cada
 * {@link Parcela} lançada nela incrementa o valorTotal via
 * {@link #adicionarParcela(BigDecimal)}. Uma fatura por (cartaoId,
 * competencia) — quem garante isso é o caso de uso (LancarCompraUseCase),
 * não o domínio.
 */
public class Fatura {

    private final UUID id;
    private final UUID cartaoId;
    private final UUID usuarioId;
    private final YearMonth competencia;
    private final LocalDate dataFechamento;
    private final LocalDate dataVencimento;
    private BigDecimal valorTotal;
    private StatusFatura status;

    private Fatura(UUID id, UUID cartaoId, UUID usuarioId, YearMonth competencia, LocalDate dataFechamento,
                    LocalDate dataVencimento, BigDecimal valorTotal, StatusFatura status) {
        this.id = id;
        this.cartaoId = cartaoId;
        this.usuarioId = usuarioId;
        this.competencia = competencia;
        this.dataFechamento = dataFechamento;
        this.dataVencimento = dataVencimento;
        this.valorTotal = valorTotal;
        this.status = status;
    }

    public static Fatura criar(UUID cartaoId, UUID usuarioId, YearMonth competencia, LocalDate dataFechamento,
                                LocalDate dataVencimento) {
        Objects.requireNonNull(cartaoId, "cartaoId é obrigatório");
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(competencia, "competencia é obrigatória");
        Objects.requireNonNull(dataFechamento, "dataFechamento é obrigatória");
        Objects.requireNonNull(dataVencimento, "dataVencimento é obrigatória");
        return new Fatura(UUID.randomUUID(), cartaoId, usuarioId, competencia, dataFechamento, dataVencimento,
                BigDecimal.ZERO, StatusFatura.ABERTA);
    }

    /** Reconstrói uma fatura já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Fatura reconstituir(UUID id, UUID cartaoId, UUID usuarioId, YearMonth competencia,
                                       LocalDate dataFechamento, LocalDate dataVencimento, BigDecimal valorTotal,
                                       StatusFatura status) {
        return new Fatura(id, cartaoId, usuarioId, competencia, dataFechamento, dataVencimento, valorTotal, status);
    }

    /** Chamado pelo LancarCompraUseCase pra cada parcela que cai nessa fatura. */
    public void adicionarParcela(BigDecimal valor) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor da parcela precisa ser positivo");
        }
        valorTotal = valorTotal.add(valor);
    }

    /** Idempotente — fechar de novo uma fatura já FECHADA (ou PAGA) não muda nada. */
    public void fechar() {
        if (status == StatusFatura.ABERTA) {
            status = StatusFatura.FECHADA;
        }
    }

    /** Idempotente — pagar de novo uma fatura já PAGA não faz nada (o caller decide se chama o account-service de novo). */
    public void pagar() {
        status = StatusFatura.PAGA;
    }

    public boolean isAberta() {
        return status == StatusFatura.ABERTA;
    }

    public boolean isPaga() {
        return status == StatusFatura.PAGA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartaoId() {
        return cartaoId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public YearMonth getCompetencia() {
        return competencia;
    }

    public LocalDate getDataFechamento() {
        return dataFechamento;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public StatusFatura getStatus() {
        return status;
    }
}
