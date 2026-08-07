package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaEventPublisher;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CriarContaUseCase {

    private final ContaRepository contaRepository;
    private final ContaEventPublisher eventPublisher;

    public CriarContaUseCase(ContaRepository contaRepository, ContaEventPublisher eventPublisher) {
        this.contaRepository = contaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Conta executar(CriarContaCommand command) {
        Conta conta = Conta.criar(
                command.usuarioId(),
                command.nome(),
                command.tipo(),
                command.saldoInicial(),
                command.instituicao()
        );
        contaRepository.salvar(conta);
        eventPublisher.publicarContaCriada(conta);
        return conta;
    }
}
