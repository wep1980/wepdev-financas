package br.com.wepdev.financas.card.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Independente de {@code TipoConta.CARTAO_CREDITO} do
 * account-service (ver ADR-0022) — {@code contaPagamentoId} é só uma
 * referência lógica pra conta que paga a fatura, confirmada de forma
 * síncrona contra o account-service pelos casos de uso, nunca uma FK real
 * (database-per-service, ADR-0001).
 */
public class Cartao {

    private final UUID id;
    private final UUID usuarioId;
    private String apelido;
    private Bandeira bandeira;
    private BigDecimal limite;
    private int diaFechamento;
    private int diaVencimento;
    private UUID contaPagamentoId;
    private boolean ativo;
    private final Instant criadoEm;

    private Cartao(UUID id, UUID usuarioId, String apelido, Bandeira bandeira, BigDecimal limite,
                    int diaFechamento, int diaVencimento, UUID contaPagamentoId, boolean ativo, Instant criadoEm) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.apelido = apelido;
        this.bandeira = bandeira;
        this.limite = limite;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
        this.contaPagamentoId = contaPagamentoId;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
    }

    public static Cartao criar(UUID usuarioId, String apelido, Bandeira bandeira, BigDecimal limite,
                                int diaFechamento, int diaVencimento, UUID contaPagamentoId) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        Objects.requireNonNull(contaPagamentoId, "contaPagamentoId é obrigatório");
        validarCampos(apelido, limite, diaFechamento, diaVencimento);
        return new Cartao(UUID.randomUUID(), usuarioId, apelido, bandeira, limite, diaFechamento, diaVencimento,
                contaPagamentoId, true, Instant.now());
    }

    /** Reconstrói um cartão já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static Cartao reconstituir(UUID id, UUID usuarioId, String apelido, Bandeira bandeira, BigDecimal limite,
                                       int diaFechamento, int diaVencimento, UUID contaPagamentoId, boolean ativo,
                                       Instant criadoEm) {
        return new Cartao(id, usuarioId, apelido, bandeira, limite, diaFechamento, diaVencimento, contaPagamentoId,
                ativo, criadoEm);
    }

    /** usuarioId e ativo não são editáveis por aqui — dono não é transferível, inativação é o método inativar(). */
    public void atualizar(String apelido, Bandeira bandeira, BigDecimal limite, int diaFechamento, int diaVencimento,
                           UUID contaPagamentoId) {
        Objects.requireNonNull(contaPagamentoId, "contaPagamentoId é obrigatório");
        validarCampos(apelido, limite, diaFechamento, diaVencimento);
        this.apelido = apelido;
        this.bandeira = bandeira;
        this.limite = limite;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
        this.contaPagamentoId = contaPagamentoId;
    }

    private static void validarCampos(String apelido, BigDecimal limite, int diaFechamento, int diaVencimento) {
        Objects.requireNonNull(apelido, "apelido é obrigatório");
        Objects.requireNonNull(limite, "limite é obrigatório");
        if (apelido.isBlank()) {
            throw new IllegalArgumentException("apelido não pode ser vazio");
        }
        if (limite.signum() <= 0) {
            throw new IllegalArgumentException("limite precisa ser positivo");
        }
        if (diaFechamento < 1 || diaFechamento > 31) {
            throw new IllegalArgumentException("diaFechamento precisa estar entre 1 e 31");
        }
        if (diaVencimento < 1 || diaVencimento > 31) {
            throw new IllegalArgumentException("diaVencimento precisa estar entre 1 e 31");
        }
    }

    /** Idempotente — não afeta faturas/compras já existentes, só impede uso futuro do cartão. */
    public void inativar() {
        ativo = false;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getApelido() {
        return apelido;
    }

    public Bandeira getBandeira() {
        return bandeira;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public int getDiaFechamento() {
        return diaFechamento;
    }

    public int getDiaVencimento() {
        return diaVencimento;
    }

    public UUID getContaPagamentoId() {
        return contaPagamentoId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
