package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaNaoEncontradaException;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarFaturaUseCase {

    private final FaturaRepository faturaRepository;
    private final ParcelaRepository parcelaRepository;

    public BuscarFaturaUseCase(FaturaRepository faturaRepository, ParcelaRepository parcelaRepository) {
        this.faturaRepository = faturaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    public FaturaDetalhe executar(UUID id, UUID usuarioId) {
        Fatura fatura = faturaRepository.buscarPorId(id)
                .filter(f -> f.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new FaturaNaoEncontradaException(id));

        return new FaturaDetalhe(fatura, parcelaRepository.listarPorFatura(fatura.getId()));
    }
}
