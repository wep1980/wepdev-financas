package br.com.wepdev.financas.document.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa (ADR-0028). */
public interface CardServiceClient {

    /** @throws CartaoNaoEncontradoException se cartaoId não existir ou não pertencer ao usuário do token propagado. */
    List<CompraExistente> listarComprasAtivas(UUID cartaoId);

    /** @throws CartaoNaoEncontradoException se cartaoId não existir ou não pertencer ao usuário do token propagado. */
    void lancarCompra(UUID cartaoId, String descricao, BigDecimal valorTotal, String categoria, LocalDate dataCompra,
                       int quantidadeParcelas);
}
