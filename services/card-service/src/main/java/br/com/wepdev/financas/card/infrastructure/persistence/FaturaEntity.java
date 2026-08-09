package br.com.wepdev.financas.card.infrastructure.persistence;

import br.com.wepdev.financas.card.domain.StatusFatura;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Modelo de persistência (JPA) — deliberadamente separado de
 * {@link br.com.wepdev.financas.card.domain.Fatura}. `competencia` é
 * gravada como String "AAAA-MM" (YearMonth.toString() já produz esse
 * formato) — só o mapper conhece essa conversão.
 */
@Entity
@Table(name = "faturas")
public class FaturaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cartao_id", length = 36, nullable = false)
    public UUID cartaoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 36, nullable = false)
    public UUID usuarioId;

    @Column(length = 7, nullable = false)
    public String competencia;

    @Column(name = "data_fechamento", nullable = false)
    public LocalDate dataFechamento;

    @Column(name = "data_vencimento", nullable = false)
    public LocalDate dataVencimento;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    public BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public StatusFatura status;
}
