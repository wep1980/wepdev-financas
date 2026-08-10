package br.com.wepdev.financas.ai.infrastructure.security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES/GCM com a chave lida de configuração (nunca hardcoded fora do
 * default de dev — ver application.properties). Usado só pra
 * {@code ConfiguracaoIa.apiKey} (API key da OpenAI por usuário, ver
 * security.md: "Banco do ai-service, campo criptografado"). IV
 * aleatório de 12 bytes por chamada, prefixado ao texto cifrado antes do
 * Base64 — não precisa ser guardado à parte.
 */
@ApplicationScoped
public class CriptografiaService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV_BYTES = 12;
    private static final int TAMANHO_TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom geradorAleatorio = new SecureRandom();

    public CriptografiaService(@ConfigProperty(name = "ai-service.criptografia.chave") String chaveBase64) {
        this.chave = new SecretKeySpec(Base64.getDecoder().decode(chaveBase64), "AES");
    }

    public String criptografar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANHO_IV_BYTES];
            geradorAleatorio.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao criptografar", e);
        }
    }

    public String descriptografar(String textoCriptografado) {
        if (textoCriptografado == null) {
            return null;
        }
        try {
            byte[] dados = Base64.getDecoder().decode(textoCriptografado);
            byte[] iv = Arrays.copyOfRange(dados, 0, TAMANHO_IV_BYTES);
            byte[] textoCifrado = Arrays.copyOfRange(dados, TAMANHO_IV_BYTES, dados.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            return new String(cipher.doFinal(textoCifrado), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao descriptografar", e);
        }
    }
}
