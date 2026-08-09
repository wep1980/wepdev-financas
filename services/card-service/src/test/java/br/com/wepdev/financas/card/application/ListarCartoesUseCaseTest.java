package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarCartoesUseCaseTest {

    private final CartaoRepository cartaoRepository = mock(CartaoRepository.class);
    private final ListarCartoesUseCase useCase = new ListarCartoesUseCase(cartaoRepository);

    @Test
    void deveriaRetornarCartoesAtivosDoUsuario() {
        UUID usuarioId = UUID.randomUUID();
        Cartao cartao = Cartao.criar(usuarioId, "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"),
                5, 12, UUID.randomUUID());
        when(cartaoRepository.listarAtivos(usuarioId)).thenReturn(List.of(cartao));

        List<Cartao> resultado = useCase.executar(usuarioId);

        assertThat(resultado).containsExactly(cartao);
    }

    @Test
    void deveriaRetornarListaVazia_quandoUsuarioSemCartoes() {
        UUID usuarioId = UUID.randomUUID();
        when(cartaoRepository.listarAtivos(usuarioId)).thenReturn(List.of());

        assertThat(useCase.executar(usuarioId)).isEmpty();
    }
}
