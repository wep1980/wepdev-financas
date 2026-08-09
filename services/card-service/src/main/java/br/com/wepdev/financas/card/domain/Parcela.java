package br.com.wepdev.financas.card.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma parcela de uma compra, associada a exatamente uma {@link Fatura}.
 * Não existe uma classe "Compra" persistida — uma compra é só o
 * agrupamento lógico de todas as Parcelas que compartilham o mesmo
 * {@code compraId} (ver LancarCompraUseCase).
 */
public class Parcela {

    private final UUID id;
    private final UUID faturaId;
    private final UUID compraId;
    private final String descricao;
    private final BigDecimal valor;
    private final String categoria;
    private final int numeroParcela;
    private final int quantidadeParcelas;

    private Parcela(UUID id, UUID faturaId, UUID compraId, String descricao, BigDecimal valor, String categoria,
                     int numeroParcela, int quantidadeParcelas) {
        this.id = id;
        this.faturaId = faturaId;
        this.compraId = compraId;
        this.descricao = descricao;
        this.valor = valor;
        this.categoria = categoria;
        this.numeroParcela = numeroParcela;
        this.quantidadeParcelas = quantidadeParcelas;
    }

    public static Parcela criar(UUID faturaId, UUID compraId, String descricao, BigDecimal valor, String categoria,
                                 int numeroParcela, int quantidadeParcelas) {
        Objects.requireNonNull(faturaId, "faturaId é obrigatório");
        Objects.requireNonNull(compraId, "compraId é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
        if (numeroParcela < 1 || numeroParcela > quantidadeParcelas) {
            throw new IllegalArgumentException("numeroParcela precisa estar entre 1 e quantidadeParcelas");
        }
        return new Parcela(UUID.randomUUID(), faturaId, compraId, descricao, valor, categoria, numeroParcela,
                quantidadeParcelas);
    }

    /** Reconstrói uma parcela já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Parcela reconstituir(UUID id, UUID faturaId, UUID compraId, String descricao, BigDecimal valor,
                                        String categoria, int numeroParcela, int quantidadeParcelas) {
        return new Parcela(id, faturaId, compraId, descricao, valor, categoria, numeroParcela, quantidadeParcelas);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFaturaId() {
        return faturaId;
    }

    public UUID getCompraId() {
        return compraId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getNumeroParcela() {
        return numeroParcela;
    }

    public int getQuantidadeParcelas() {
        return quantidadeParcelas;
    }
}
