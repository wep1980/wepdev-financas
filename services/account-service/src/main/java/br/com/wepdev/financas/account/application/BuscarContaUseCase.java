package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarContaUseCase {

    private final ContaRepository contaRepository;

    public BuscarContaUseCase(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    /**
     * Conta de outro usuário é tratada igual a inexistente (404, não 403) —
     * evita confirmar pra quem não é dono que o id existe (IDOR).
     */
    public Conta executar(UUID id, UUID usuarioId) {
        return contaRepository.buscarPorId(id)
                .filter(conta -> conta.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
    }
}
