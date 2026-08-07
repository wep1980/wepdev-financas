package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaRepository;
import br.com.wepdev.financas.account.domain.TipoConta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarContasUseCaseTest {

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final ListarContasUseCase useCase = new ListarContasUseCase(contaRepository);

    @Test
    void deveriaRetornarContasAtivasDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        Conta conta = Conta.criar(usuarioId, "Conta corrente", TipoConta.CORRENTE, BigDecimal.TEN, "Banco X");
        when(contaRepository.listarAtivasPorUsuario(eq(usuarioId))).thenReturn(List.of(conta));

        List<Conta> resultado = useCase.executar(usuarioId);

        assertThat(resultado).containsExactly(conta);
    }

    @Test
    void deveriaRetornarListaVazia_quandoUsuarioSemContas() {
        UUID usuarioId = UUID.randomUUID();
        when(contaRepository.listarAtivasPorUsuario(eq(usuarioId))).thenReturn(List.of());

        List<Conta> resultado = useCase.executar(usuarioId);

        assertThat(resultado).isEmpty();
    }
}
