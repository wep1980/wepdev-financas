package br.com.wepdev.financas.document.infrastructure.parsing.dto;

import java.util.List;

/**
 * Formato-objeto (não array solto) pedido ao LLM — testado na prática
 * (2026-08-09) que um array JSON no nível raiz faz o modelo, às vezes,
 * "colapsar" um resultado de um único lançamento num objeto solto em vez de
 * lista de um item. Envelopar num objeto com campo nomeado evitou esse
 * comportamento. {@code anoReferencia} existe porque lançamentos de fatura
 * geralmente só têm dia/mês no texto — pedir o ano como campo separado (uma
 * extração simples) é mais confiável que pedir pro LLM montar a data
 * completa por lançamento.
 */
public record FaturaExtraidaDto(String anoReferencia, List<LancamentoExtraidoDto> lancamentos) {
}
