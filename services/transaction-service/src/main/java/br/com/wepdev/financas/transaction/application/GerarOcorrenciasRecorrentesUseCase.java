package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDate;

/**
 * Núcleo do job agendado (ver GerarOcorrenciasRecorrentesJob) — recebe
 * "hoje" como parâmetro em vez de ler o relógio do sistema, pra poder ser
 * testado com datas controladas, sem Thread.sleep nem tempo real.
 */
@ApplicationScoped
public class GerarOcorrenciasRecorrentesUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final RegistrarTransacaoUseCase registrarTransacaoUseCase;

    public GerarOcorrenciasRecorrentesUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository,
                                               RegistrarTransacaoUseCase registrarTransacaoUseCase) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.registrarTransacaoUseCase = registrarTransacaoUseCase;
    }

    /**
     * Gera no máximo UMA ocorrência por regra ATIVA vencida nessa execução
     * — se o job ficar dias sem rodar, o atraso é recuperado incrementalmente
     * a cada execução seguinte, em vez de gerar tudo de uma vez (mais simples
     * e sem risco de laço indefinido numa única chamada).
     */
    @Transactional
    public int executar(LocalDate hoje) {
        int geradas = 0;
        for (TransacaoRecorrente regra : transacaoRecorrenteRepository.listarAtivas()) {
            if (!regra.proximaDataVencimento().isAfter(hoje)) {
                gerarOcorrencia(regra);
                geradas++;
            }
        }
        return geradas;
    }

    private void gerarOcorrencia(TransacaoRecorrente regra) {
        registrarTransacaoUseCase.executar(new RegistrarTransacaoCommand(
                regra.getContaId(),
                regra.getUsuarioId(),
                regra.getDescricao(),
                regra.getValor(),
                regra.getTipo(),
                regra.getCategoria(),
                regra.proximaDataVencimento(),
                regra.getId()
        ));
        regra.registrarOcorrenciaGerada();
        transacaoRecorrenteRepository.salvar(regra);
    }
}
