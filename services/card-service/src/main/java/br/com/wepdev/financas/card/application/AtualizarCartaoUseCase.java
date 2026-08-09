package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtualizarCartaoUseCase {

    private final CartaoRepository cartaoRepository;
    private final AccountServiceClient accountServiceClient;

    public AtualizarCartaoUseCase(CartaoRepository cartaoRepository, AccountServiceClient accountServiceClient) {
        this.cartaoRepository = cartaoRepository;
        this.accountServiceClient = accountServiceClient;
    }

    /** Confirma a posse de contaPagamentoId de novo — pode ter mudado pra uma conta que não é do usuário. */
    @Transactional
    public Cartao executar(AtualizarCartaoCommand command) {
        Cartao cartao = cartaoRepository.buscarPorId(command.id())
                .filter(c -> c.getUsuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new CartaoNaoEncontradoException(command.id()));

        accountServiceClient.confirmarPosseDaConta(command.contaPagamentoId());

        cartao.atualizar(command.apelido(), command.bandeira(), command.limite(), command.diaFechamento(),
                command.diaVencimento(), command.contaPagamentoId());
        cartaoRepository.salvar(cartao);
        return cartao;
    }
}
