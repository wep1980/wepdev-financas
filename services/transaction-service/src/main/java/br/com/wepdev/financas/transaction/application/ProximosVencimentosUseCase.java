package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

/** Consumido pelo job diário do notification-service (ADR-0010) — não filtra por usuarioId, ver spec. */
@ApplicationScoped
public class ProximosVencimentosUseCase {

    private final TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    public ProximosVencimentosUseCase(TransacaoRecorrenteRepository transacaoRecorrenteRepository) {
        this.transacaoRecorrenteRepository = transacaoRecorrenteRepository;
    }

    public List<ProximoVencimento> executar(LocalDate hoje, int dias) {
        LocalDate limite = hoje.plusDays(dias);
        return transacaoRecorrenteRepository.listarAtivas().stream()
                .map(regra -> new ProximoVencimento(
                        regra.getId(),
                        regra.getUsuarioId(),
                        regra.getDescricao(),
                        regra.getValor(),
                        regra.proximaDataVencimento()
                ))
                .filter(v -> !v.dataVencimentoPrevista().isBefore(hoje) && !v.dataVencimentoPrevista().isAfter(limite))
                .toList();
    }
}
