package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class BuscarTransacaoRecorrenteUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public BuscarTransacaoRecorrenteUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    public TransacaoRecorrente executar(UUID id, UUID usuarioId) {
        return transacaoRecorrenteRepository.buscarPorId(id)
                .filter(r -> r.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new TransacaoRecorrenteNaoEncontradaException(id));
    }
}
