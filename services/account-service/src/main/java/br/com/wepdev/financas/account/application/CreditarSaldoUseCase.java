package br.com.wepdev.financas.account.application;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.account.domain.ContaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@ApplicationScoped
public class CreditarSaldoUseCase {

    private final ContaRepository contaRepository;

    public CreditarSaldoUseCase(ContaRepository contaRepository) {
        this.contaRepository = contaRepository;
    }

    @Transactional
    public Conta executar(UUID contaId, BigDecimal valor) {
        Conta conta = contaRepository.buscarPorId(contaId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contaId));
        conta.creditar(valor);
        contaRepository.salvar(conta);
        return conta;
    }
}
