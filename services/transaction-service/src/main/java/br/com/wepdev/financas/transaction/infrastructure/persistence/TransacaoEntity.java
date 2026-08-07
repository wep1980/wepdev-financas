package br.com.wepdev.financas.transaction.infrastructure.persistence;

import br.com.wepdev.financas.transaction.domain.StatusTransacao;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de persistência (JPA) — deliberadamente separado de
 * {@link br.com.wepdev.financas.transaction.domain.Transacao}. O domínio não
 * sabe que Hibernate existe; só o mapper (TransacaoMapper) conhece os dois lados.
 */
@Entity
@Table(name = "transacoes")
public class TransacaoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "conta_id", length = 36, nullable = false)
    public UUID contaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public TipoTransacao tipo;

    public String categoria;

    @Column(name = "data_transacao", nullable = false)
    public LocalDate dataTransacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public StatusTransacao status;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "transacao_recorrente_id", length = 36)
    public UUID transacaoRecorrenteId;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm;
}
