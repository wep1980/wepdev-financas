package br.com.wepdev.financas.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída (Dependency Inversion): o domínio define o contrato,
 * a infraestrutura implementa. Nenhuma classe de domínio/aplicação conhece
 * Panache/JPA diretamente.
 */
public interface ContaRepository {

    void salvar(Conta conta);

    Optional<Conta> buscarPorId(UUID id);

    List<Conta> listarAtivasPorUsuario(UUID usuarioId);
}
