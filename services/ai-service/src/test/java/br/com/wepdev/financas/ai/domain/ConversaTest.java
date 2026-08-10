package br.com.wepdev.financas.ai.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversaTest {

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaIniciarConversaSemMensagens() {
        Conversa conversa = Conversa.iniciar(usuarioId);

        assertThat(conversa.getId()).isNotNull();
        assertThat(conversa.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(conversa.getMensagens()).isEmpty();
        assertThat(conversa.getAcaoPendente()).isNull();
    }

    @Test
    void deveriaAdicionarMensagensNaOrdem() {
        Conversa conversa = Conversa.iniciar(usuarioId);

        conversa.adicionarMensagemUsuario("Quanto posso gastar esse mês?");
        conversa.adicionarRespostaAgente("Você tem R$800 disponível.", TipoRespostaAgente.RESPOSTA);

        assertThat(conversa.getMensagens()).hasSize(2);
        assertThat(conversa.getMensagens().get(0).getAutor()).isEqualTo(AutorMensagem.USUARIO);
        assertThat(conversa.getMensagens().get(1).getAutor()).isEqualTo(AutorMensagem.AGENTE);
        assertThat(conversa.getMensagens().get(1).getTipo()).isEqualTo(TipoRespostaAgente.RESPOSTA);
    }

    @Test
    void getUltimaAtividadeEm_deveriaSerIniciadaEm_quandoSemMensagens() {
        Conversa conversa = Conversa.iniciar(usuarioId);

        assertThat(conversa.getUltimaAtividadeEm()).isEqualTo(conversa.getIniciadaEm());
    }

    @Test
    void getUltimaAtividadeEm_deveriaSerDaUltimaMensagem() {
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.adicionarMensagemUsuario("Oi");

        assertThat(conversa.getUltimaAtividadeEm()).isEqualTo(conversa.getMensagens().get(0).getCriadaEm());
    }

    @Test
    void deveriaProporEConfirmarAcaoPendente() {
        Conversa conversa = Conversa.iniciar(usuarioId);
        AcaoPendente acao = AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false, null, null, null, null);

        conversa.proporAcao(acao);
        assertThat(conversa.temAcaoPendenteValida(Instant.now())).isTrue();

        AcaoPendente confirmada = conversa.confirmarAcaoPendente(Instant.now());

        assertThat(confirmada).isEqualTo(acao);
        assertThat(conversa.getAcaoPendente()).isNull();
        assertThat(conversa.temAcaoPendenteValida(Instant.now())).isFalse();
    }

    @Test
    void deveriaSubstituirPropostaAnterior_quandoNovaAcaoProposta() {
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false, null, null, null, null));
        AcaoPendente corrigida = AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("200.00"), false, null, null, null, null);

        conversa.proporAcao(corrigida);

        assertThat(conversa.getAcaoPendente()).isEqualTo(corrigida);
    }

    @Test
    void deveriaLancarExcecao_quandoConfirmarSemAcaoPendente() {
        Conversa conversa = Conversa.iniciar(usuarioId);

        assertThatThrownBy(() -> conversa.confirmarAcaoPendente(Instant.now()))
                .isInstanceOf(NenhumaAcaoPendenteException.class);
    }

    @Test
    void deveriaLancarExcecaoELimparAcao_quandoConfirmarAcaoExpirada() {
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(AcaoPendente.propor(TipoTransacao.RECEITA, "Mercado", new BigDecimal("50.00"), false, null, null, null, null));
        Instant depoisDeExpirar = Instant.now().plus(11, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> conversa.confirmarAcaoPendente(depoisDeExpirar))
                .isInstanceOf(AcaoPendenteExpiradaException.class);
        assertThat(conversa.getAcaoPendente()).isNull();
    }

    @Test
    void deveriaLimparAcaoPendente_semExecutar() {
        Conversa conversa = Conversa.iniciar(usuarioId);
        conversa.proporAcao(AcaoPendente.propor(TipoTransacao.DESPESA, "Mercado", new BigDecimal("100.00"), false, null, null, null, null));

        conversa.limparAcaoPendente();

        assertThat(conversa.getAcaoPendente()).isNull();
    }

    @Test
    void deveriaReconstituirConversaExistente() {
        UUID id = UUID.randomUUID();
        Instant iniciadaEm = Instant.now().minusSeconds(60);
        List<Mensagem> mensagens = List.of(Mensagem.doUsuario("Oi"));

        Conversa conversa = Conversa.reconstituir(id, usuarioId, iniciadaEm, mensagens, null);

        assertThat(conversa.getId()).isEqualTo(id);
        assertThat(conversa.getMensagens()).hasSize(1);
    }
}
