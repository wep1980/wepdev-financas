package br.com.wepdev.financas.ai.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object, sempre embutido numa {@link Conversa} — resumo estruturado
 * de uma ação de escrita proposta pelo agente, aguardando confirmação
 * explícita do usuário (ADR-0007). Expira sozinha depois de um tempo
 * curto, pra evitar confirmação tardia agindo sobre contexto
 * desatualizado.
 */
public final class AcaoPendente {

    private static final Duration DURACAO_VALIDADE = Duration.ofMinutes(10);

    private final TipoTransacao tipo;
    private final String descricao;
    private final BigDecimal valor;
    private final boolean recorrente;
    private final FrequenciaRecorrencia frequencia;
    private final Integer quantidadeOcorrencias;
    private final UUID contaId;
    private final String categoria;
    private final Instant criadaEm;
    private final Instant expiraEm;

    private AcaoPendente(TipoTransacao tipo, String descricao, BigDecimal valor, boolean recorrente,
                          FrequenciaRecorrencia frequencia, Integer quantidadeOcorrencias, UUID contaId,
                          String categoria, Instant criadaEm, Instant expiraEm) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.recorrente = recorrente;
        this.frequencia = frequencia;
        this.quantidadeOcorrencias = quantidadeOcorrencias;
        this.contaId = contaId;
        this.categoria = categoria;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public static AcaoPendente propor(TipoTransacao tipo, String descricao, BigDecimal valor, boolean recorrente,
                                       FrequenciaRecorrencia frequencia, Integer quantidadeOcorrencias,
                                       UUID contaId, String categoria) {
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        validarDescricao(descricao);
        validarValor(valor);
        if (recorrente) {
            Objects.requireNonNull(frequencia, "frequencia é obrigatória pra ação recorrente");
        }
        Instant agora = Instant.now();
        return new AcaoPendente(tipo, descricao, valor, recorrente, frequencia, quantidadeOcorrencias, contaId,
                categoria, agora, agora.plus(DURACAO_VALIDADE));
    }

    /** Reconstrói uma ação pendente já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static AcaoPendente reconstituir(TipoTransacao tipo, String descricao, BigDecimal valor, boolean recorrente,
                                             FrequenciaRecorrencia frequencia, Integer quantidadeOcorrencias,
                                             UUID contaId, String categoria, Instant criadaEm, Instant expiraEm) {
        return new AcaoPendente(tipo, descricao, valor, recorrente, frequencia, quantidadeOcorrencias, contaId,
                categoria, criadaEm, expiraEm);
    }

    private static void validarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("descricao não pode ser vazia");
        }
    }

    private static void validarValor(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
    }

    public boolean isExpirada(Instant agora) {
        return agora.isAfter(expiraEm);
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isRecorrente() {
        return recorrente;
    }

    public FrequenciaRecorrencia getFrequencia() {
        return frequencia;
    }

    public Integer getQuantidadeOcorrencias() {
        return quantidadeOcorrencias;
    }

    public UUID getContaId() {
        return contaId;
    }

    public String getCategoria() {
        return categoria;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }
}
