package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

/**
 * Núcleo do job agendado (ver FecharFaturasVencidasJob) — recebe "hoje"
 * como parâmetro em vez de ler o relógio do sistema, pra poder ser
 * testado com datas controladas, sem Thread.sleep nem tempo real. Mesmo
 * padrão do GerarOcorrenciasRecorrentesUseCase (transaction-service).
 */
@ApplicationScoped
public class FecharFaturasVencidasUseCase {

    private final FaturaRepository faturaRepository;

    public FecharFaturasVencidasUseCase(FaturaRepository faturaRepository) {
        this.faturaRepository = faturaRepository;
    }

    @Transactional
    public int executar(LocalDate hoje) {
        int fechadas = 0;
        for (Fatura fatura : faturaRepository.listarAbertasVencidas(hoje)) {
            fatura.fechar();
            faturaRepository.salvar(fatura);
            fechadas++;
        }
        return fechadas;
    }
}
