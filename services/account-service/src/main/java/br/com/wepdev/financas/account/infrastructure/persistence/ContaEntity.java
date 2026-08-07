package br.com.wepdev.financas.account.infrastructure.persistence;

import br.com.wepdev.financas.account.domain.TipoConta;
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
 * {@link br.com.wepdev.financas.account.domain.Conta}. O domínio não sabe
 * que Hibernate existe; só o mapper (ContaMapper) conhece os dois lados.
 */
@Entity
@Table(name = "contas")
public class ContaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public TipoConta tipo;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal saldo;

    public String instituicao;

    @Column(nullable = false)
    public boolean ativa;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;
}
