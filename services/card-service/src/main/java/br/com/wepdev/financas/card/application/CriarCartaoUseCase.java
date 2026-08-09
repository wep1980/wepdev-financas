package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.AccountServiceClient;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CriarCartaoUseCase {

    private final CartaoRepository cartaoRepository;
    private final AccountServiceClient accountServiceClient;

    public CriarCartaoUseCase(CartaoRepository cartaoRepository, AccountServiceClient accountServiceClient) {
        this.cartaoRepository = cartaoRepository;
        this.accountServiceClient = accountServiceClient;
    }

    /** Confirma a posse de contaPagamentoId contra o account-service ANTES de persistir — sem cartão "órfão" de conta inválida. */
    @Transactional
    public Cartao executar(CriarCartaoCommand command) {
        accountServiceClient.confirmarPosseDaConta(command.contaPagamentoId());

        Cartao cartao = Cartao.criar(
                command.usuarioId(),
                command.apelido(),
                command.bandeira(),
                command.limite(),
                command.diaFechamento(),
                command.diaVencimento(),
                command.contaPagamentoId()
        );
        cartaoRepository.salvar(cartao);
        return cartao;
    }
}
