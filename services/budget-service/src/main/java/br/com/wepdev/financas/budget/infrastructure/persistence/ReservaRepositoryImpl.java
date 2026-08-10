package br.com.wepdev.financas.budget.infrastructure.persistence;

import br.com.wepdev.financas.budget.domain.Reserva;
import br.com.wepdev.financas.budget.domain.ReservaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReservaRepositoryImpl implements ReservaRepository {

    private final ReservaPanacheRepository panacheRepository;

    public ReservaRepositoryImpl(ReservaPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void salvar(Reserva reserva) {
        ReservaEntity entity = panacheRepository.findById(reserva.getUsuarioId());
        if (entity == null) {
            panacheRepository.persist(ReservaMapper.paraNovaEntidade(reserva));
        } else {
            ReservaMapper.atualizarEntidade(entity, reserva);
        }
    }

    @Override
    public Optional<Reserva> buscarPorUsuario(UUID usuarioId) {
        return panacheRepository.findByIdOptional(usuarioId).map(ReservaMapper::paraDominio);
    }
}
