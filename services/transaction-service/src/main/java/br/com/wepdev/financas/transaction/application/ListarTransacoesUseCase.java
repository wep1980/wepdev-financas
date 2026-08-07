package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ListarTransacoesUseCase {

    private final TransacaoRepository transacaoRepository;

    public ListarTransacoesUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public List<Transacao> executar(TransacaoFiltro filtro) {
        return transacaoRepository.listar(filtro);
    }
}
