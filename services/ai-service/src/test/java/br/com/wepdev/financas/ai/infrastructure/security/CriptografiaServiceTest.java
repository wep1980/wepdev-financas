package br.com.wepdev.financas.ai.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CriptografiaServiceTest {

    // Chave de teste, gerada só pra esse teste — não é a de dev/prod.
    private static final String CHAVE_TESTE_BASE64 = Base64.getEncoder().encodeToString(new byte[32]);

    private final CriptografiaService service = new CriptografiaService(CHAVE_TESTE_BASE64);

    @Test
    void deveriaCriptografarEDescriptografar_devolvendoOTextoOriginal() {
        String textoPlano = "sk-teste-chave-super-secreta-123";

        String criptografado = service.criptografar(textoPlano);
        String descriptografado = service.descriptografar(criptografado);

        assertThat(criptografado).isNotEqualTo(textoPlano);
        assertThat(descriptografado).isEqualTo(textoPlano);
    }

    @Test
    void deveriaGerarTextosCriptografadosDiferentes_paraOMesmoTextoPlano() {
        String textoPlano = "sk-teste-chave-super-secreta-123";

        String primeiraVez = service.criptografar(textoPlano);
        String segundaVez = service.criptografar(textoPlano);

        assertThat(primeiraVez).isNotEqualTo(segundaVez);
    }

    @Test
    void deveriaDevolverNulo_quandoEntradaNula() {
        assertThat(service.criptografar(null)).isNull();
        assertThat(service.descriptografar(null)).isNull();
    }
}
