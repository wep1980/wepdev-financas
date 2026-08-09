package br.com.wepdev.financas.card.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Modelo de persistência (JPA) — deliberadamente separado de
 * {@link br.com.wepdev.financas.card.domain.Parcela}.
 */
@Entity
@Table(name = "parcelas")
public class ParcelaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "fatura_id", length = 36, nullable = false)
    public UUID faturaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "compra_id", length = 36, nullable = false)
    public UUID compraId;

    @Column(nullable = false)
    public String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal valor;

    public String categoria;

    @Column(name = "numero_parcela", nullable = false)
    public int numeroParcela;

    @Column(name = "quantidade_parcelas", nullable = false)
    public int quantidadeParcelas;
}
