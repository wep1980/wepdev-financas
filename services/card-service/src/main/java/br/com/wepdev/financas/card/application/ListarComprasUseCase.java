package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.Parcela;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import br.com.wepdev.financas.card.domain.StatusFatura;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base pro dedup do document-service (2026-08-11 — uma compra já
 * conhecida de um upload anterior não deve ser lançada de novo, ver
 * historico.md) e pras perguntas de IA sobre parcelamento (quantas
 * compras parceladas, maior parcela, quanto falta pra quitar uma compra
 * — ai-service/AgenteOrquestradorUseCase). Não existe tabela "compras":
 * agrupa as {@link Parcela}s de todas as faturas do cartão por
 * {@code compraId} (mesmo padrão N+1 já usado em
 * {@code DocumentoRepositoryImpl.listarPorUsuario} no document-service —
 * volume baixo o bastante pra não justificar uma query agregada agora).
 */
@ApplicationScoped
public class ListarComprasUseCase {

    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;
    private final ParcelaRepository parcelaRepository;

    public ListarComprasUseCase(CartaoRepository cartaoRepository, FaturaRepository faturaRepository,
                                 ParcelaRepository parcelaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    public List<CompraResumo> executar(UUID cartaoId, UUID usuarioId) {
        cartaoRepository.buscarPorId(cartaoId)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new CartaoNaoEncontradoException(cartaoId));

        List<Fatura> faturas = faturaRepository.listarPorCartao(cartaoId, null);
        Map<UUID, StatusFatura> statusPorFatura = new LinkedHashMap<>();
        for (Fatura fatura : faturas) {
            statusPorFatura.put(fatura.getId(), fatura.getStatus());
        }

        Map<UUID, List<Parcela>> parcelasPorCompra = new LinkedHashMap<>();
        for (Fatura fatura : faturas) {
            for (Parcela parcela : parcelaRepository.listarPorFatura(fatura.getId())) {
                parcelasPorCompra.computeIfAbsent(parcela.getCompraId(), k -> new ArrayList<>()).add(parcela);
            }
        }

        List<CompraResumo> resumos = new ArrayList<>();
        for (Map.Entry<UUID, List<Parcela>> entrada : parcelasPorCompra.entrySet()) {
            resumos.add(resumir(cartaoId, entrada.getKey(), entrada.getValue(), statusPorFatura));
        }
        return resumos.stream()
                .sorted(Comparator.comparing(CompraResumo::descricao))
                .toList();
    }

    private CompraResumo resumir(UUID cartaoId, UUID compraId, List<Parcela> parcelas,
                                  Map<UUID, StatusFatura> statusPorFatura) {
        Parcela primeira = parcelas.stream()
                .min(Comparator.comparingInt(Parcela::getNumeroParcela))
                .orElseThrow();

        List<Parcela> emAberto = parcelas.stream()
                .filter(p -> statusPorFatura.get(p.getFaturaId()) == StatusFatura.ABERTA)
                .toList();
        BigDecimal valorTotalRestante = emAberto.stream()
                .map(Parcela::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CompraResumo(compraId, cartaoId, primeira.getDescricao(), primeira.getCategoria(),
                primeira.getValor(), primeira.getQuantidadeParcelas(), emAberto.size(), valorTotalRestante,
                emAberto.isEmpty());
    }
}
