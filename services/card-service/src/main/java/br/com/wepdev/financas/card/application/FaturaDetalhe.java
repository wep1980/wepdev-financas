package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.Parcela;

import java.util.List;

public record FaturaDetalhe(Fatura fatura, List<Parcela> parcelas) {
}
