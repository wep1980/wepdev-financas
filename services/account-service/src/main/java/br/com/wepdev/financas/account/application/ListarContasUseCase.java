package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarContasUseCase {

    private final ContaRepository contaRepository;

    public ListarContasUseCase(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    public List<Conta> executar(UUID usuarioId) {
        return contaRepository.listarAtivasPorUsuario(usuarioId);
    }
}
