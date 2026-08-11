package br.com.wepdev.financas.document.infrastructure.persistence;

import br.com.wepdev.financas.document.domain.StatusLancamento;
import br.com.wepdev.financas.document.domain.TipoLancamento;
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
 * {@link br.com.wepdev.financas.document.domain.LancamentoPendente}.
 * {@code documentoId} referencia o id do {@code DocumentoImportadoEntity}
 * no MongoDB — nunca uma FK real (bancos diferentes).
 */
@Entity
@Table(name = "lancamentos_pendentes")
public class LancamentoPendenteEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    public UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "documento_id", length = 36, nullable = false)
    public UUID documentoId;

    @Column(nullable = false)
    public String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal valor;

    @Column(nullable = false)
    public LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    public TipoLancamento tipo;

    @Column(name = "categoria_sugerida")
    public String categoriaSugerida;

    @Column(name = "numero_parcela", nullable = false)
    public int numeroParcela;

    @Column(name = "quantidade_parcelas", nullable = false)
    public int quantidadeParcelas;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    public StatusLancamento status;
}
