package br.com.wepdev.financas.ai.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracaoIaTest {

    private final UUID usuarioId = UUID.randomUUID();

    @Test
    void deveriaDefinirOllama_semApiKey() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.definir(usuarioId, ProvedorIa.OLLAMA, null, "http://localhost:11434");

        assertThat(configuracao.getProvedor()).isEqualTo(ProvedorIa.OLLAMA);
        assertThat(configuracao.getApiKey()).isNull();
        assertThat(configuracao.isConfigurado()).isTrue();
    }

    @Test
    void deveriaDefinirOpenAi_comApiKey() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.definir(usuarioId, ProvedorIa.OPENAI, "sk-teste123", null);

        assertThat(configuracao.getProvedor()).isEqualTo(ProvedorIa.OPENAI);
        assertThat(configuracao.getApiKey()).isEqualTo("sk-teste123");
    }

    @Test
    void deveriaLancarExcecao_quandoOpenAiSemApiKey() {
        assertThatThrownBy(() -> ConfiguracaoIa.definir(usuarioId, ProvedorIa.OPENAI, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConfiguracaoIa.definir(usuarioId, ProvedorIa.OPENAI, "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveriaCriarSemDefinir_comProvedorNenhum() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.semDefinir(usuarioId);

        assertThat(configuracao.getProvedor()).isEqualTo(ProvedorIa.NENHUM);
        assertThat(configuracao.isConfigurado()).isFalse();
    }

    @Test
    void deveriaAtualizarDeOllamaParaOpenAi() {
        ConfiguracaoIa configuracao = ConfiguracaoIa.definir(usuarioId, ProvedorIa.OLLAMA, null, null);

        configuracao.atualizar(ProvedorIa.OPENAI, "sk-nova", null);

        assertThat(configuracao.getProvedor()).isEqualTo(ProvedorIa.OPENAI);
        assertThat(configuracao.getApiKey()).isEqualTo("sk-nova");
    }
}
