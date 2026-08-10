package br.com.wepdev.financas.document.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade filha de {@link DocumentoImportado} — persistida à parte, em
 * MySQL (o agregado inteiro vive em dois bancos: metadados do documento no
 * Mongo, lançamentos aqui, ver {@code docs/architecture/overview.md}).
 */
public class LancamentoPendente {

    private final UUID id;
    private final UUID documentoId;
    private final String descricao;
    private final BigDecimal valor;
    private final LocalDate data;
    private final TipoLancamento tipo;
    private final String categoriaSugerida;
    private StatusLancamento status;

    private LancamentoPendente(UUID id, UUID documentoId, String descricao, BigDecimal valor, LocalDate data,
                                TipoLancamento tipo, String categoriaSugerida, StatusLancamento status) {
        this.id = id;
        this.documentoId = documentoId;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
        this.categoriaSugerida = categoriaSugerida;
        this.status = status;
    }

    /** Nasce PENDENTE — resultado bruto da extração, antes de qualquer confirmação do usuário. */
    public static LancamentoPendente extrair(UUID documentoId, String descricao, BigDecimal valor, LocalDate data,
                                              TipoLancamento tipo, String categoriaSugerida) {
        Objects.requireNonNull(documentoId, "documentoId é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(data, "data é obrigatória");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        if (descricao.isBlank()) {
            throw new IllegalArgumentException("descricao não pode ser vazia");
        }
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
        return new LancamentoPendente(UUID.randomUUID(), documentoId, descricao, valor, data, tipo, categoriaSugerida,
                StatusLancamento.PENDENTE);
    }

    /** Reconstrói um lançamento já existente (vindo da persistência) — não valida como se fosse extração nova. */
    public static LancamentoPendente reconstituir(UUID id, UUID documentoId, String descricao, BigDecimal valor,
                                                   LocalDate data, TipoLancamento tipo, String categoriaSugerida,
                                                   StatusLancamento status) {
        return new LancamentoPendente(id, documentoId, descricao, valor, data, tipo, categoriaSugerida, status);
    }

    void confirmar() {
        status = StatusLancamento.CONFIRMADO;
    }

    void rejeitar() {
        status = StatusLancamento.REJEITADO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentoId() {
        return documentoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getData() {
        return data;
    }

    public TipoLancamento getTipo() {
        return tipo;
    }

    public String getCategoriaSugerida() {
        return categoriaSugerida;
    }

    public StatusLancamento getStatus() {
        return status;
    }
}
