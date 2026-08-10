package br.com.wepdev.financas.ai.infrastructure.persistence;

import br.com.wepdev.financas.ai.domain.AcaoPendente;
import br.com.wepdev.financas.ai.domain.AutorMensagem;
import br.com.wepdev.financas.ai.domain.Conversa;
import br.com.wepdev.financas.ai.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.ai.domain.Mensagem;
import br.com.wepdev.financas.ai.domain.TipoRespostaAgente;
import br.com.wepdev.financas.ai.domain.TipoTransacao;

final class ConversaMapper {

    private ConversaMapper() {
    }

    static ConversaEntity paraNovaEntidade(Conversa conversa) {
        ConversaEntity entity = new ConversaEntity();
        entity.id = conversa.getId();
        atualizarEntidade(entity, conversa);
        return entity;
    }

    static void atualizarEntidade(ConversaEntity entity, Conversa conversa) {
        entity.usuarioId = conversa.getUsuarioId();
        entity.iniciadaEm = conversa.getIniciadaEm();
        entity.mensagens = conversa.getMensagens().stream().map(ConversaMapper::paraEmbedded).toList();
        entity.acaoPendente = conversa.getAcaoPendente() == null ? null : paraEmbedded(conversa.getAcaoPendente());
    }

    static Conversa paraDominio(ConversaEntity entity) {
        return Conversa.reconstituir(
                entity.id,
                entity.usuarioId,
                entity.iniciadaEm,
                entity.mensagens.stream().map(ConversaMapper::paraDominio).toList(),
                entity.acaoPendente == null ? null : paraDominio(entity.acaoPendente)
        );
    }

    private static MensagemEmbedded paraEmbedded(Mensagem mensagem) {
        MensagemEmbedded embedded = new MensagemEmbedded();
        embedded.autor = mensagem.getAutor().name();
        embedded.texto = mensagem.getTexto();
        embedded.tipo = mensagem.getTipo() == null ? null : mensagem.getTipo().name();
        embedded.criadaEm = mensagem.getCriadaEm();
        return embedded;
    }

    private static Mensagem paraDominio(MensagemEmbedded embedded) {
        return Mensagem.reconstituir(
                AutorMensagem.valueOf(embedded.autor),
                embedded.texto,
                embedded.tipo == null ? null : TipoRespostaAgente.valueOf(embedded.tipo),
                embedded.criadaEm
        );
    }

    private static AcaoPendenteEmbedded paraEmbedded(AcaoPendente acao) {
        AcaoPendenteEmbedded embedded = new AcaoPendenteEmbedded();
        embedded.tipo = acao.getTipo().name();
        embedded.descricao = acao.getDescricao();
        embedded.valor = acao.getValor();
        embedded.recorrente = acao.isRecorrente();
        embedded.frequencia = acao.getFrequencia() == null ? null : acao.getFrequencia().name();
        embedded.quantidadeOcorrencias = acao.getQuantidadeOcorrencias();
        embedded.contaId = acao.getContaId();
        embedded.categoria = acao.getCategoria();
        embedded.criadaEm = acao.getCriadaEm();
        embedded.expiraEm = acao.getExpiraEm();
        return embedded;
    }

    private static AcaoPendente paraDominio(AcaoPendenteEmbedded embedded) {
        return AcaoPendente.reconstituir(
                TipoTransacao.valueOf(embedded.tipo),
                embedded.descricao,
                embedded.valor,
                embedded.recorrente,
                embedded.frequencia == null ? null : FrequenciaRecorrencia.valueOf(embedded.frequencia),
                embedded.quantidadeOcorrencias,
                embedded.contaId,
                embedded.categoria,
                embedded.criadaEm,
                embedded.expiraEm
        );
    }
}
