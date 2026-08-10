package br.com.wepdev.financas.budget.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Modelo de persistência (JPA) — deliberadamente separado de
 * {@link br.com.wepdev.financas.budget.domain.Reserva}. Uma linha por
 * usuário (usuarioId é a própria chave primária, não um id gerado à
 * parte) — não é um histórico, é configuração de valor único (ADR-0026).
 */
@Entity
@Table(name = "reservas")
public class ReservaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36)
    public UUID usuarioId;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal valor;

    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;
}
