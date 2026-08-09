package br.com.wepdev.financas.card.domain;

import java.util.List;
import java.util.UUID;

/** Porta de saída (Dependency Inversion) — domínio define o contrato, infraestrutura implementa. */
public interface ParcelaRepository {

    void salvar(Parcela parcela);

    List<Parcela> listarPorFatura(UUID faturaId);
}
