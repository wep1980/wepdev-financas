package br.com.wepdev.financas.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root do domínio de contas. Regras de negócio (saldo nunca fica
 * negativo, exclusão é sempre lógica) vivem aqui, não na camada de
 * persistência ou REST.
 */
public class Conta {

    private final UUID id;
    private final UUID usuarioId;
    private String nome;
    private final TipoConta tipo;
    private BigDecimal saldo;
    private String instituicao;
    private boolean ativa;
    private final Instant criadoEm;
    private Instant atualizadoEm;

    private Conta(UUID id, UUID usuarioId, String nome, TipoConta tipo, BigDecimal saldo,
                  String instituicao, boolean ativa, Instant criadoEm, Instant atualizadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.tipo = tipo;
        this.saldo = saldo;
        this.instituicao = instituicao;
        this.ativa = ativa;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public static Conta criar(UUID usuarioId, String nome, TipoConta tipo, BigDecimal saldoInicial, String instituicao) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(nome, "nome é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        BigDecimal saldo = saldoInicial == null ? BigDecimal.ZERO : saldoInicial;
        if (saldo.signum() < 0) {
            throw new IllegalArgumentException("saldoInicial não pode ser negativo");
        }
        Instant agora = Instant.now();
        return new Conta(UUID.randomUUID(), usuarioId, nome, tipo, saldo, instituicao, true, agora, agora);
    }

    /** Reconstrói uma conta já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Conta reconstituir(UUID id, UUID usuarioId, String nome, TipoConta tipo, BigDecimal saldo,
                                      String instituicao, boolean ativa, Instant criadoEm, Instant atualizadoEm) {
        return new Conta(id, usuarioId, nome, tipo, saldo, instituicao, ativa, criadoEm, atualizadoEm);
    }

    public void debitar(BigDecimal valor) {
        exigirValorPositivo(valor);
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException(id, saldo, valor);
        }
        saldo = saldo.subtract(valor);
        atualizadoEm = Instant.now();
    }

    public void creditar(BigDecimal valor) {
        exigirValorPositivo(valor);
        saldo = saldo.add(valor);
        atualizadoEm = Instant.now();
    }

    public void atualizar(String nome, String instituicao) {
        Objects.requireNonNull(nome, "nome é obrigatório");
        this.nome = nome;
        this.instituicao = instituicao;
        this.atualizadoEm = Instant.now();
    }

    public void inativar() {
        ativa = false;
        atualizadoEm = Instant.now();
    }

    private void exigirValorPositivo(BigDecimal valor) {
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("valor precisa ser positivo");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public String getInstituicao() {
        return instituicao;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
