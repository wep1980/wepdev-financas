package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class ExcluirContaUseCase {

    private final ContaRepository contaRepository;

    public ExcluirContaUseCase(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    /** Exclusão lógica (CLAUDE.md princípio 5) — histórico financeiro nunca some fisicamente. */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        Conta conta = contaRepository.buscarPorId(id)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        conta.inativar();
        contaRepository.salvar(conta);
    }
}
