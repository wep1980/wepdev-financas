package br.com.wepdev.financas.ai.infrastructure.llm;

import br.com.wepdev.financas.ai.domain.ChatRequest;
import br.com.wepdev.financas.ai.domain.IaNaoConfiguradaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProvedorNaoConfiguradoLlmProviderTest {

    private final ProvedorNaoConfiguradoLlmProvider provider = new ProvedorNaoConfiguradoLlmProvider();

    @Test
    void isConfigured_deveriaSerFalse() {
        assertThat(provider.isConfigured()).isFalse();
    }

    @Test
    void chat_deveriaLancarIaNaoConfiguradaException() {
        assertThatThrownBy(() -> provider.chat(ChatRequest.deTexto("oi")))
                .isInstanceOf(IaNaoConfiguradaException.class);
    }

    @Test
    void embed_deveriaLancarIaNaoConfiguradaException() {
        assertThatThrownBy(() -> provider.embed("texto"))
                .isInstanceOf(IaNaoConfiguradaException.class);
    }
}
