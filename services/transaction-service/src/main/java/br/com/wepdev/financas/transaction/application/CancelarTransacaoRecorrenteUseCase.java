package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class CancelarTransacaoRecorrenteUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public CancelarTransacaoRecorrenteUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    /** Idempotente. Só impede novas ocorrências — não afeta Transacao já geradas (ADR-0009). */
    @Transactional
    public void executar(UUID id, UUID usuarioId) {
        TransacaoRecorrente regra = transacaoRecorrenteRepository.buscarPorId(id)
                .filter(r -> r.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new TransacaoRecorrenteNaoEncontradaException(id));

        if (regra.isCancelada()) {
            return;
        }

        regra.cancelar();
        transacaoRecorrenteRepository.salvar(regra);
    }
}
