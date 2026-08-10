package br.com.wepdev.financas.budget.application;

import br.com.wepdev.financas.budget.domain.AccountServiceClient;
import br.com.wepdev.financas.budget.domain.CardServiceClient;
import br.com.wepdev.financas.budget.domain.Conta;
import br.com.wepdev.financas.budget.domain.DespesaRecorrente;
import br.com.wepdev.financas.budget.domain.FaturaFechada;
import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import br.com.wepdev.financas.budget.domain.TransactionServiceClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalcularDisponivelParaGastarUseCaseTest {

    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final CardServiceClient cardServiceClient = mock(CardServiceClient.class);
    private final TransactionServiceClient transactionServiceClient = mock(TransactionServiceClient.class);
    private final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    private final CalcularDisponivelParaGastarUseCase useCase = new CalcularDisponivelParaGastarUseCase(
            accountServiceClient, cardServiceClient, transactionServiceClient, reservaRepository);

    private final UUID usuarioId = UUID.randomUUID();
    private final YearMonth mesReferencia = YearMonth.of(2026, 8);

    @Test
    void deveriaCalcularDisponivel_aplicandoFormulaCompleta() {
        when(accountServiceClient.buscarContasAtivas()).thenReturn(List.of(
                new Conta(UUID.randomUUID(), "Corrente", "CORRENTE", new BigDecimal("3000.00")),
                new Conta(UUID.randomUUID(), "Carteira", "CARTEIRA", new BigDecimal("100.00")),
                new Conta(UUID.randomUUID(), "Poupança", "POUPANCA", new BigDecimal("10000.00"))
        ));
        when(cardServiceClient.buscarFaturasFechadas()).thenReturn(List.of(
                new FaturaFechada(UUID.randomUUID(), "Nubank", new BigDecimal("500.00"), LocalDate.of(2026, 8, 10)),
                new FaturaFechada(UUID.randomUUID(), "Itaú", new BigDecimal("300.00"), LocalDate.of(2026, 9, 5))
        ));
        when(transactionServiceClient.buscarDespesasRecorrentesAtivas()).thenReturn(List.of(
                new DespesaRecorrente(UUID.randomUUID(), "Aluguel", new BigDecimal("1500.00"), LocalDate.of(2026, 1, 1)),
                new DespesaRecorrente(UUID.randomUUID(), "Academia", new BigDecimal("100.00"), LocalDate.of(2026, 9, 1))
        ));
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.of(Reserva.definir(usuarioId, new BigDecimal("200.00"))));

        DisponivelParaGastarResultado resultado = useCase.executar(usuarioId, mesReferencia);

        // saldoContas: só CORRENTE + CARTEIRA (poupança fica de fora) = 3000 + 100 = 3100
        assertThat(resultado.saldoContas()).isEqualByComparingTo("3100.00");
        // faturasEmAberto: só a de vencimento em agosto (a de setembro fica de fora) = 500
        assertThat(resultado.faturasEmAberto()).isEqualByComparingTo("500.00");
        // despesasRecorrentes: só a com dataInicio <= fim de agosto (academia começa em setembro, fica de fora) = 1500
        assertThat(resultado.despesasRecorrentes()).isEqualByComparingTo("1500.00");
        assertThat(resultado.reserva()).isEqualByComparingTo("200.00");
        // 3100 - 500 - 1500 - 200 = 900
        assertThat(resultado.valorDisponivel()).isEqualByComparingTo("900.00");
        assertThat(resultado.contas()).hasSize(2);
        assertThat(resultado.faturas()).hasSize(1);
        assertThat(resultado.despesasRecorrentesAtivas()).hasSize(1);
        assertThat(resultado.mesReferencia()).isEqualTo(mesReferencia);
    }

    @Test
    void deveriaUsarReservaZero_quandoUsuarioNuncaDefiniu() {
        when(accountServiceClient.buscarContasAtivas()).thenReturn(List.of());
        when(cardServiceClient.buscarFaturasFechadas()).thenReturn(List.of());
        when(transactionServiceClient.buscarDespesasRecorrentesAtivas()).thenReturn(List.of());
        when(reservaRepository.buscarPorUsuario(usuarioId)).thenReturn(Optional.empty());

        DisponivelParaGastarResultado resultado = useCase.executar(usuarioId, mesReferencia);

        assertThat(resultado.reserva()).isEqualByComparingTo("0");
        assertThat(resultado.valorDisponivel()).isEqualByComparingTo("0");
    }
}
