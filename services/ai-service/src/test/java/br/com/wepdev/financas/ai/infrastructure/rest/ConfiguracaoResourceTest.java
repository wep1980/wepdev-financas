package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.infrastructure.rest.dto.ConfiguracaoIaRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ConfiguracaoResourceTest {

    private static final String SUB_NUNCA_CONFIGUROU = "a1500000-0000-4000-8000-0000000000b1";
    private static final String SUB_OLLAMA = "a1500000-0000-4000-8000-0000000000b2";
    private static final String SUB_OPENAI = "a1500000-0000-4000-8000-0000000000b3";

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_NUNCA_CONFIGUROU))
    void deveriaDevolverNenhumConfigurado_quandoUsuarioNuncaDefiniu() {
        given()
        .when()
                .get("/api/v1/configuracao")
        .then()
                .statusCode(200)
                .body("provedor", equalTo("NENHUM"))
                .body("configurado", equalTo(false));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_OLLAMA))
    void deveriaDefinirOllama_semApiKey() {
        ConfiguracaoIaRequest request = new ConfiguracaoIaRequest(
                br.com.wepdev.financas.ai.domain.ProvedorIa.OLLAMA, null, null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/configuracao")
        .then()
                .statusCode(200)
                .body("provedor", equalTo("OLLAMA"))
                .body("configurado", equalTo(true));

        given()
        .when()
                .get("/api/v1/configuracao")
        .then()
                .statusCode(200)
                .body("provedor", equalTo("OLLAMA"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_OPENAI))
    void deveriaDefinirOpenAi_eNuncaDevolverApiKey() {
        ConfiguracaoIaRequest request = new ConfiguracaoIaRequest(
                br.com.wepdev.financas.ai.domain.ProvedorIa.OPENAI, "sk-teste-123", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/configuracao")
        .then()
                .statusCode(200)
                .body("provedor", equalTo("OPENAI"))
                .body("configurado", equalTo(true))
                .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("apiKey")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_OPENAI))
    void deveriaRetornar400_quandoOpenAiSemApiKey() {
        ConfiguracaoIaRequest request = new ConfiguracaoIaRequest(
                br.com.wepdev.financas.ai.domain.ProvedorIa.OPENAI, null, null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/configuracao")
        .then()
                .statusCode(400);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
        .when()
                .get("/api/v1/configuracao")
        .then()
                .statusCode(401);
    }
}
