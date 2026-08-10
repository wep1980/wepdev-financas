package br.com.wepdev.financas.budget.infrastructure.persistence;

import br.com.wepdev.financas.budget.domain.StatusOrcamento;
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
 * {@link br.com.wepdev.financas.budget.domain.Orcamento}. O domínio não
 * sabe que Hibernate existe; só o mapper (OrcamentoMapper) conhece os dois
 * lados. {@code mesReferencia} vira String (formato AAAA-MM) aqui — mesmo
 * padrão de {@code competencia} em FaturaEntity do card-service.
 */
@Entity
@Table(name = "orcamentos")
public class OrcamentoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    public UUID usuarioId;

    @Column(nullable = false)
    public String categoria;

    @Column(name = "mes_referencia", length = 7, nullable = false)
    public String mesReferencia;

    @Column(name = "valor_limite", nullable = false, precision = 19, scale = 2)
    public BigDecimal valorLimite;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    public StatusOrcamento status;

    @Column(name = "criado_em", nullable = false)
    public Instant criadoEm;
}
