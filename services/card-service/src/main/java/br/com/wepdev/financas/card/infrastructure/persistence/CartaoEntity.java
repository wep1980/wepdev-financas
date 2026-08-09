package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.Bandeira;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de persistência (JPA) — deliberadamente separado de
 * {@link br.com.wepdev.financas.card.domain.Cartao}. O domínio não sabe que
 * Hibernate existe; só o mapper (CartaoMapper) conhece os dois lados.
 */
@Entity
@Table(name = "cartoes")
public class CartaoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String apelido;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    public Bandeira bandeira;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal limite;

    @Column(name = "dia_fechamento", nullable = false)
    public int diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    public int diaVencimento;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "conta_pagamento_id", length = 36, nullable = false)
    public UUID contaPagamentoId;

    @Column(nullable = false)
    public boolean ativo;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm;
}
