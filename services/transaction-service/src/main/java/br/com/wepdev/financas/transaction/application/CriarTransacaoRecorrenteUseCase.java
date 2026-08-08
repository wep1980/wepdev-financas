package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CriarTransacaoRecorrenteUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;
    private final RegistrarTransacaoUseCase registrarTransacaoUseCase;

    public CriarTransacaoRecorrenteUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository,
                                            RegistrarTransacaoUseCase registrarTransacaoUseCase) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
        this.registrarTransacaoUseCase = registrarTransacaoUseCase;
    }

    /**
     * Cria a regra e gera a 1ª ocorrência imediatamente (síncrono com
     * account-service, mesmo caminho de uma transação avulsa — ver
     * RegistrarTransacaoUseCase). Se quantidadeOcorrencias == 1, a regra já
     * nasce CONCLUIDA depois dessa única ocorrência.
     */
    @Transactional
    public TransacaoRecorrente executar(CriarTransacaoRecorrenteCommand command) {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(
                command.contaId(),
                command.usuarioId(),
                command.descricao(),
                command.valor(),
                command.tipo(),
                command.categoria(),
                command.frequencia(),
                command.dataInicio(),
                command.quantidadeOcorrencias()
        );

        registrarTransacaoUseCase.executar(new RegistrarTransacaoCommand(
                regra.getContaId(),
                regra.getUsuarioId(),
                regra.getDescricao(),
                regra.getValor(),
                regra.getTipo(),
                regra.getCategoria(),
                regra.proximaDataVencimento(),
                regra.getId()
        ));
        regra.registrarOcorrenciaGerada();

        transacaoRecorrenteRepository.salvar(regra);
        return regra;
    }
}
