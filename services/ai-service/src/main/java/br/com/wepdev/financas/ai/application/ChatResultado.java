package br.com.wepdev.financas.ai.application;

import br.com.wepdev.financas.ai.domain.AcaoPendente;
import br.com.wepdev.financas.ai.domain.TipoRespostaAgente;

import java.util.List;
import java.util.UUID;

/** acaoProposta preenchido só quando tipo=PROPOSTA_ACAO. */
public record ChatResultado(UUID conversaId, String resposta, TipoRespostaAgente tipo, AcaoPendente acaoProposta,
                             List<RegistroTrace> trace) {
}
