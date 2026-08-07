package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AtualizarContaUseCase {

    private final ContaRepository contaRepository;

    public AtualizarContaUseCase(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    @Transactional
    public Conta executar(AtualizarContaCommand command) {
        Conta conta = contaRepository.buscarPorId(command.id())
                .filter(c -> c.getUsuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new ContaNaoEncontradaException(command.id()));
        conta.atualizar(command.nome(), command.instituicao());
        contaRepository.salvar(conta);
        return conta;
    }
}
