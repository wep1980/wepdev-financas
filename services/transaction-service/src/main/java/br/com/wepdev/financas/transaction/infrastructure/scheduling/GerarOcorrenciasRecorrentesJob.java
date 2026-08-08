package br.com.wepdev.financas.transaction.infrastructure.scheduling;

import br.com.wepdev.financas.transaction.application.GerarOcorrenciasRecorrentesUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

/**
 * Wrapper fino: só liga o relógio real (LocalDate.now()) ao caso de uso.
 * Toda a lógica testável fica em GerarOcorrenciasRecorrentesUseCase, que
 * recebe a data como parâmetro — desabilitado em teste
 * (%test.quarkus.scheduler.enabled=false) pra não rodar em paralelo com os
 * testes, que chamam o caso de uso direto com datas controladas.
 */
@ApplicationScoped
public class GerarOcorrenciasRecorrentesJob {

    private final GerarOcorrenciasRecorrentesUseCase gerarOcorrenciasRecorrentesUseCase;

    public GerarOcorrenciasRecorrentesJob(GerarOcorrenciasRecorrentesUseCase gerarOcorrenciasRecorrentesUseCase) {
        this.gerarOcorrenciasRecorrentesUseCase = gerarOcorrenciasRecorrentesUseCase;
    }

    @Scheduled(cron = "0 5 0 * * ?")
    void executar() {
        gerarOcorrenciasRecorrentesUseCase.executar(LocalDate.now());
    }
}
