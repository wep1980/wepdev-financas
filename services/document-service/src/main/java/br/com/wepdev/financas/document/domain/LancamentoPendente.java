package br.com.wepdev.financas.document.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Entidade filha de {@link DocumentoImportado} — persistida à parte, em
 * MySQL (o agregado inteiro vive em dois bancos: metadados do documento no
 * Mongo, lançamentos aqui, ver {@code docs/architecture/overview.md}).
 */
public class LancamentoPendente {

    /** Ex: "Mercadolivre*Mercadol - Parcela 8/11" → "Mercadolivre*Mercadol" (ver getDescricaoBase, ADR-0028). */
    private static final Pattern SUFIXO_PARCELA =
            Pattern.compile("\\s*-?\\s*Parcela\\s+\\d+\\s*/\\s*\\d+\\s*$", Pattern.CASE_INSENSITIVE);

    private final UUID id;
    private final UUID documentoId;
    private final String descricao;
    private final BigDecimal valor;
    private final LocalDate data;
    private final TipoLancamento tipo;
    private final String categoriaSugerida;
    private final int numeroParcela;
    private final int quantidadeParcelas;
    private StatusLancamento status;

    private LancamentoPendente(UUID id, UUID documentoId, String descricao, BigDecimal valor, LocalDate data,
                                TipoLancamento tipo, String categoriaSugerida, int numeroParcela,
                                int quantidadeParcelas, StatusLancamento status) {
        this.id = id;
        this.documentoId = documentoId;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
        this.categoriaSugerida = categoriaSugerida;
        this.numeroParcela = numeroParcela;
        this.quantidadeParcelas = quantidadeParcelas;
        this.status = status;
    }

    /**
     * Nasce PENDENTE — resultado bruto da extração, antes de qualquer
     * confirmação do usuário. {@code numeroParcela}/{@code quantidadeParcelas}
     * 1/1 = compra à vista (ver {@code LancamentoExtraidoDto}, que já
     * resolve o padrão "Parcela X/Y" antes de chegar aqui — 2026-08-11,
     * base do religamento com o card-service, ver ADR-0028).
     */
    public static LancamentoPendente extrair(UUID documentoId, String descricao, BigDecimal valor, LocalDate data,
                                              TipoLancamento tipo, String categoriaSugerida, int numeroParcela,
                                              int quantidadeParcelas) {
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
        if (quantidadeParcelas < 1) {
            throw new IllegalArgumentException("quantidadeParcelas precisa ser pelo menos 1");
        }
        if (numeroParcela < 1 || numeroParcela > quantidadeParcelas) {
            throw new IllegalArgumentException("numeroParcela precisa estar entre 1 e quantidadeParcelas");
        }
        return new LancamentoPendente(UUID.randomUUID(), documentoId, descricao, valor, data, tipo, categoriaSugerida,
                numeroParcela, quantidadeParcelas, StatusLancamento.PENDENTE);
    }

    /** Reconstrói um lançamento já existente (vindo da persistência) — não valida como se fosse extração nova. */
    public static LancamentoPendente reconstituir(UUID id, UUID documentoId, String descricao, BigDecimal valor,
                                                   LocalDate data, TipoLancamento tipo, String categoriaSugerida,
                                                   int numeroParcela, int quantidadeParcelas,
                                                   StatusLancamento status) {
        return new LancamentoPendente(id, documentoId, descricao, valor, data, tipo, categoriaSugerida,
                numeroParcela, quantidadeParcelas, status);
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

    public int getNumeroParcela() {
        return numeroParcela;
    }

    public int getQuantidadeParcelas() {
        return quantidadeParcelas;
    }

    public boolean isParcelado() {
        return quantidadeParcelas > 1;
    }

    /**
     * Descrição sem o sufixo "- Parcela X/Y" (se houver) — usada como
     * descrição canônica ao lançar a compra no card-service e como parte
     * da assinatura de dedup (a numeração já vira campo estruturado, não
     * precisa continuar em texto livre). Sem sufixo reconhecido, retorna
     * a descrição original.
     */
    public String getDescricaoBase() {
        return SUFIXO_PARCELA.matcher(descricao).replaceAll("").trim();
    }

    public StatusLancamento getStatus() {
        return status;
    }
}
