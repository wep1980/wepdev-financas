package br.com.wepdev.financas.ai.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Não é um aggregate no sentido tradicional — é uma configuração de valor
 * único por usuário (ADR-0002), sem histórico, sempre um upsert por
 * {@code usuarioId} (mesmo desenho de {@code Reserva} no budget-service).
 * {@code apiKey} aqui é sempre texto plano — a criptografia acontece na
 * borda da persistência (ver infrastructure.security.CriptografiaService),
 * o domínio não sabe que isso existe.
 */
public class ConfiguracaoIa {

    private final UUID usuarioId;
    private ProvedorIa provedor;
    private String apiKey;
    private String ollamaUrl;

    private ConfiguracaoIa(UUID usuarioId, ProvedorIa provedor, String apiKey, String ollamaUrl) {
        this.usuarioId = usuarioId;
        this.provedor = provedor;
        this.apiKey = apiKey;
        this.ollamaUrl = ollamaUrl;
    }

    public static ConfiguracaoIa definir(UUID usuarioId, ProvedorIa provedor, String apiKey, String ollamaUrl) {
        Objects.requireNonNull(usuarioId, "usuarioId é obrigatório");
        validarProvedor(provedor, apiKey);
        return new ConfiguracaoIa(usuarioId, provedor, apiKey, ollamaUrl);
    }

    /** Reconstrói uma configuração já existente (vinda da persistência) — não valida como se fosse criação nova. */
    public static ConfiguracaoIa reconstituir(UUID usuarioId, ProvedorIa provedor, String apiKey, String ollamaUrl) {
        return new ConfiguracaoIa(usuarioId, provedor, apiKey, ollamaUrl);
    }

    /** Estado default de quem nunca configurou um provedor. */
    public static ConfiguracaoIa semDefinir(UUID usuarioId) {
        return new ConfiguracaoIa(usuarioId, ProvedorIa.NENHUM, null, null);
    }

    public void atualizar(ProvedorIa provedor, String apiKey, String ollamaUrl) {
        validarProvedor(provedor, apiKey);
        this.provedor = provedor;
        this.apiKey = apiKey;
        this.ollamaUrl = ollamaUrl;
    }

    private static void validarProvedor(ProvedorIa provedor, String apiKey) {
        Objects.requireNonNull(provedor, "provedor é obrigatório");
        if (provedor == ProvedorIa.OPENAI && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("apiKey é obrigatória pra provedor OPENAI");
        }
    }

    public boolean isConfigurado() {
        return provedor != ProvedorIa.NENHUM;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public ProvedorIa getProvedor() {
        return provedor;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getOllamaUrl() {
        return ollamaUrl;
    }
}
