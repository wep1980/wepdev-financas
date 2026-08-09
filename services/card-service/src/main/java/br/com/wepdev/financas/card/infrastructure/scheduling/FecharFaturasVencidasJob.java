package br.com.wepdev.financas.card.infrastructure.scheduling;

import br.com.wepdev.financas.card.application.FecharFaturasVencidasUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

/**
 * Wrapper fino: só liga o relógio real (LocalDate.now()) ao caso de uso.
 * Toda a lógica testável fica em FecharFaturasVencidasUseCase, que recebe
 * a data como parâmetro — desabilitado em teste
 * (%test.quarkus.scheduler.enabled=false) pra não rodar em paralelo com
 * os testes, que chamam o caso de uso direto com datas controladas.
 */
@ApplicationScoped
public class FecharFaturasVencidasJob {

    private final FecharFaturasVencidasUseCase fecharFaturasVencidasUseCase;

    public FecharFaturasVencidasJob(FecharFaturasVencidasUseCase fecharFaturasVencidasUseCase) {
        this.fecharFaturasVencidasUseCase = fecharFaturasVencidasUseCase;
    }

    @Scheduled(cron = "0 10 0 * * ?")
    void executar() {
        fecharFaturasVencidasUseCase.executar(LocalDate.now());
    }
}
