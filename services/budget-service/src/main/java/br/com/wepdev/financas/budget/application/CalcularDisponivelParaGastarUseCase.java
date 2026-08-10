package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.AccountServiceClient;
import br.com.wepdev.financas.budget.domain.CardServiceClient;
import br.com.wepdev.financas.budget.domain.Conta;
import br.com.wepdev.financas.budget.domain.DespesaRecorrente;
import br.com.wepdev.financas.budget.domain.FaturaFechada;
import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Calcula "disponível pra gastar" no mês (PRD 3.3) — regra exata e o
 * porquê de cada parcela em ADR-0026. Três chamadas síncronas de leitura
 * (account-service/card-service/transaction-service, token do usuário
 * propagado) + a reserva local — nenhuma escrita, não é transacional.
 */
@ApplicationScoped
public class CalcularDisponivelParaGastarUseCase {

    private static final Set<String> TIPOS_CONTA_DISPONIVEL = Set.of("CORRENTE", "CARTEIRA");

    private final AccountServiceClient accountServiceClient;
    private final CardServiceClient cardServiceClient;
    private final TransactionServiceClient transactionServiceClient;
    private final ReservaRepository reservaRepository;

    public CalcularDisponivelParaGastarUseCase(AccountServiceClient accountServiceClient,
                                                CardServiceClient cardServiceClient,
                                                TransactionServiceClient transactionServiceClient,
                                                ReservaRepository reservaRepository) {
        this.accountServiceClient = accountServiceClient;
        this.cardServiceClient = cardServiceClient;
        this.transactionServiceClient = transactionServiceClient;
        this.reservaRepository = reservaRepository;
    }

    public DisponivelParaGastarResultado executar(UUID usuarioId, YearMonth mesReferencia) {
        LocalDate inicioDoMes = mesReferencia.atDay(1);
        LocalDate fimDoMes = mesReferencia.atEndOfMonth();

        List<Conta> contas = accountServiceClient.buscarContasAtivas().stream()
                .filter(conta -> TIPOS_CONTA_DISPONIVEL.contains(conta.tipo()))
                .toList();

        List<FaturaFechada> faturas = cardServiceClient.buscarFaturasFechadas().stream()
                .filter(fatura -> !fatura.dataVencimento().isBefore(inicioDoMes) && !fatura.dataVencimento().isAfter(fimDoMes))
                .toList();

        List<DespesaRecorrente> despesasRecorrentes = transactionServiceClient.buscarDespesasRecorrentesAtivas().stream()
                .filter(despesa -> !despesa.dataInicio().isAfter(fimDoMes))
                .toList();

        BigDecimal reserva = reservaRepository.buscarPorUsuario(usuarioId)
                .map(Reserva::getValor)
                .orElse(BigDecimal.ZERO);

        BigDecimal saldoContas = somar(contas.stream().map(Conta::saldo));
        BigDecimal faturasEmAberto = somar(faturas.stream().map(FaturaFechada::valorTotal));
        BigDecimal totalDespesasRecorrentes = somar(despesasRecorrentes.stream().map(DespesaRecorrente::valor));

        BigDecimal valorDisponivel = saldoContas
                .subtract(faturasEmAberto)
                .subtract(totalDespesasRecorrentes)
                .subtract(reserva);

        return new DisponivelParaGastarResultado(mesReferencia, saldoContas, faturasEmAberto,
                totalDespesasRecorrentes, reserva, valorDisponivel, contas, faturas, despesasRecorrentes);
    }

    private static BigDecimal somar(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
