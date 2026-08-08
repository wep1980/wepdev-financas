package br.com.wepdev.financas.transaction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root — regra que gera {@link Transacao}s ao longo do tempo
 * (ex: salário mensal, assinatura). Distinta de parcelamento de cartão,
 * que é conceito próprio do card-service (ADR-0009).
 */
public class TransacaoRecorrente {

    private final UUID id;
    private final UUID contaId;
    private final UUID usuarioId;
    private final String descricao;
    private final BigDecimal valor;
    private final TipoTransacao tipo;
    private final String categoria;
    private final FrequenciaRecorrencia frequencia;
    private final LocalDate dataInicio;
    private final Integer quantidadeOcorrencias;
    private int ocorrenciasGeradas;
    private StatusTransacaoRecorrente status;
    private final Instant criadoEm;

    private TransacaoRecorrente(UUID id, UUID contaId, UUID usuarioId, String descricao, BigDecimal valor,
                                 TipoTransacao tipo, String categoria, FrequenciaRecorrencia frequencia,
                                 LocalDate dataInicio, Integer quantidadeOcorrencias, int ocorrenciasGeradas,
                                 StatusTransacaoRecorrente status, Instant criadoEm) {
        this.id = id;
        this.contaId = contaId;
        this.usuarioId = usuarioId;
        this.descricao = descricao;
        this.valor = valor;
        this.tipo = tipo;
        this.categoria = categoria;
        this.frequencia = frequencia;
        this.dataInicio = dataInicio;
        this.quantidadeOcorrencias = quantidadeOcorrencias;
        this.ocorrenciasGeradas = ocorrenciasGeradas;
        this.status = status;
        this.criadoEm = criadoEm;
    }

    public static TransacaoRecorrente criar(UUID contaId, UUID usuarioId, String descricao, BigDecimal valor,
                                             TipoTransacao tipo, String categoria, FrequenciaRecorrencia frequencia,
                                             LocalDate dataInicio, Integer quantidadeOcorrencias) {
        Objects.requireNonNull(contaId, "contaId é obrigatório");
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(descricao, "descricao é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(frequencia, "frequencia é obrigatória");
        Objects.requireNonNull(dataInicio, "dataInicio é obrigatória");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
        if (quantidadeOcorrencias != null && quantidadeOcorrencias <= 0) {
            throw new IllegalArgumentException("quantidadeOcorrencias precisa ser positiva quando informada");
        }
        return new TransacaoRecorrente(UUID.randomUUID(), contaId, usuarioId, descricao, valor, tipo, categoria,
                frequencia, dataInicio, quantidadeOcorrencias, 0, StatusTransacaoRecorrente.ATIVA, Instant.now());
    }

    /** Reconstrói uma regra já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static TransacaoRecorrente reconstituir(UUID id, UUID contaId, UUID usuarioId, String descricao,
                                                    BigDecimal valor, TipoTransacao tipo, String categoria,
                                                    FrequenciaRecorrencia frequencia, LocalDate dataInicio,
                                                    Integer quantidadeOcorrencias, int ocorrenciasGeradas,
                                                    StatusTransacaoRecorrente status, Instant criadoEm) {
        return new TransacaoRecorrente(id, contaId, usuarioId, descricao, valor, tipo, categoria, frequencia,
                dataInicio, quantidadeOcorrencias, ocorrenciasGeradas, status, criadoEm);
    }

    /** Data prevista da próxima ocorrência ainda não gerada. */
    public LocalDate proximaDataVencimento() {
        return switch (frequencia) {
            case MENSAL -> dataInicio.plusMonths(ocorrenciasGeradas);
        };
    }

    /** Chamado depois que uma Transacao foi gerada pra essa regra — conclui automaticamente ao atingir o limite. */
    public void registrarOcorrenciaGerada() {
        ocorrenciasGeradas++;
        if (quantidadeOcorrencias != null && ocorrenciasGeradas >= quantidadeOcorrencias) {
            status = StatusTransacaoRecorrente.CONCLUIDA;
        }
    }

    /** Idempotente — não afeta ocorrências (Transacao) já geradas, só impede novas. */
    public void cancelar() {
        status = StatusTransacaoRecorrente.CANCELADA;
    }

    public boolean isAtiva() {
        return status == StatusTransacaoRecorrente.ATIVA;
    }

    public boolean isCancelada() {
        return status == StatusTransacaoRecorrente.CANCELADA;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContaId() {
        return contaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public FrequenciaRecorrencia getFrequencia() {
        return frequencia;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public Integer getQuantidadeOcorrencias() {
        return quantidadeOcorrencias;
    }

    public int getOcorrenciasGeradas() {
        return ocorrenciasGeradas;
    }

    public StatusTransacaoRecorrente getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
