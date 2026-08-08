package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.StatusTransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ListarTransacoesRecorrentesUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public ListarTransacoesRecorrentesUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    public List<TransacaoRecorrente> executar(UUID usuarioId, StatusTransacaoRecorrente status) {
        return transacaoRecorrenteRepository.listar(usuarioId, status);
    }
}
