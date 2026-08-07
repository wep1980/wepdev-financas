package br.com.wepdev.financas.account.infrastructure.rest;

import br.com.wepdev.financas.account.domain.Conta;
import br.com.wepdev.financas.account.domain.ContaRepository;
import br.com.wepdev.financas.account.domain.TipoConta;
import br.com.wepdev.financas.account.infrastructure.rest.dto.AtualizarContaRequest;
import br.com.wepdev.financas.account.infrastructure.rest.dto.CriarContaRequest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class ContaResourceTest {

    private static final String SUB_USUARIO_TESTE = "a10c1000-0000-4000-8000-000000000001";
    // subs exclusivos dos testes de listagem — precisam de contagem exata,
    // não podem compartilhar usuário com os demais testes da classe (mesma
    // base entre métodos, sem rollback automático).
    private static final String SUB_LISTAGEM_1 = "a10c1000-0000-4000-8000-000000000002";
    private static final String SUB_LISTAGEM_2 = "a10c1000-0000-4000-8000-000000000003";

    @Inject
    ContaRepository contaRepository;

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCriarConta_quandoRequisicaoValida() {
        CriarContaRequest request = new CriarContaRequest(
                "Conta corrente", TipoConta.CORRENTE, new BigDecimal("150.00"), "Banco X"
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/contas")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("usuarioId", equalTo(SUB_USUARIO_TESTE))
                .body("nome", equalTo("Conta corrente"))
                .body("tipo", equalTo("CORRENTE"))
                .body("ativa", equalTo(true));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400ComCampoDoErro_quandoNomeAusente() {
        String jsonInvalido = """
                {"tipo": "CORRENTE"}
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/contas")
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("nome"));
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        CriarContaRequest request = new CriarContaRequest(
                "Conta corrente", TipoConta.CORRENTE, BigDecimal.ZERO, null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/contas")
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM_1))
    void deveriaListarContasDoUsuarioAutenticado() {
        criarContaEObterId(new BigDecimal("50.00"));

        given()
        .when()
                .get("/api/v1/contas")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].usuarioId", equalTo(SUB_LISTAGEM_1));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM_2))
    void naoDeveriaListarContaDeOutroUsuario() {
        criarContaDireto(UUID.randomUUID(), new BigDecimal("50.00"));

        given()
        .when()
                .get("/api/v1/contas")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaBuscarContaPorId_quandoContaExisteEEhDoUsuario() {
        String id = criarContaEObterId(new BigDecimal("50.00"));

        given()
        .when()
                .get("/api/v1/contas/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoContaNaoExiste() {
        given()
        .when()
                .get("/api/v1/contas/{id}", UUID.randomUUID())
        .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoContaEhDeOutroUsuario() {
        UUID id = criarContaDireto(UUID.randomUUID(), new BigDecimal("50.00"));

        given()
        .when()
                .get("/api/v1/contas/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaAtualizarConta_quandoUsuarioEhDono() {
        String id = criarContaEObterId(new BigDecimal("50.00"));

        given()
                .contentType(ContentType.JSON)
                .body(new AtualizarContaRequest("Nome novo", "Banco Y"))
        .when()
                .put("/api/v1/contas/{id}", id)
        .then()
                .statusCode(200)
                .body("nome", equalTo("Nome novo"))
                .body("instituicao", equalTo("Banco Y"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoAtualizarComNomeVazio() {
        String id = criarContaEObterId(new BigDecimal("50.00"));

        given()
                .contentType(ContentType.JSON)
                .body(new AtualizarContaRequest("", "Banco Y"))
        .when()
                .put("/api/v1/contas/{id}", id)
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("nome"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoAtualizarContaDeOutroUsuario() {
        UUID id = criarContaDireto(UUID.randomUUID(), new BigDecimal("50.00"));

        given()
                .contentType(ContentType.JSON)
                .body(new AtualizarContaRequest("Nome novo", "Banco Y"))
        .when()
                .put("/api/v1/contas/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaExcluirConta_quandoUsuarioEhDono() {
        String id = criarContaEObterId(new BigDecimal("50.00"));

        given()
        .when()
                .delete("/api/v1/contas/{id}", id)
        .then()
                .statusCode(204);

        given()
        .when()
                .get("/api/v1/contas/{id}", id)
        .then()
                .statusCode(200)
                .body("ativa", equalTo(false));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoExcluirContaDeOutroUsuario() {
        UUID id = criarContaDireto(UUID.randomUUID(), new BigDecimal("50.00"));

        given()
        .when()
                .delete("/api/v1/contas/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoExcluirContaInexistente() {
        given()
        .when()
                .delete("/api/v1/contas/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "servico-teste", roles = {"usuario", "service"})
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaDebitarSaldo_quandoChamadaComRoleService() {
        String id = criarContaEObterId(new BigDecimal("100.00"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 30.00}")
        .when()
                .post("/api/v1/contas/{id}/debitos", id)
        .then()
                .statusCode(200)
                .body("saldo", equalTo(70.00f));
    }

    @Test
    @TestSecurity(user = "servico-teste", roles = {"usuario", "service"})
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoDebitoComSaldoInsuficiente() {
        String id = criarContaEObterId(new BigDecimal("10.00"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 30.00}")
        .when()
                .post("/api/v1/contas/{id}/debitos", id)
        .then()
                .statusCode(422)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "servico-teste", roles = {"usuario", "service"})
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCreditarSaldo_quandoChamadaComRoleService() {
        String id = criarContaEObterId(new BigDecimal("100.00"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 30.00}")
        .when()
                .post("/api/v1/contas/{id}/creditos", id)
        .then()
                .statusCode(200)
                .body("saldo", equalTo(130.00f));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar403_quandoDebitoChamadoComRoleUsuario() {
        String id = criarContaEObterId(new BigDecimal("100.00"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"valor\": 30.00}")
        .when()
                .post("/api/v1/contas/{id}/debitos", id)
        .then()
                .statusCode(403);
    }

    private String criarContaEObterId(BigDecimal saldoInicial) {
        CriarContaRequest request = new CriarContaRequest(
                "Conta corrente", TipoConta.CORRENTE, saldoInicial, "Banco X"
        );
        return given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/contas")
                .jsonPath().getString("id");
    }

    /** Insere direto no repositório, fora do fluxo REST, pra simular conta de outro usuário sem depender de outro token real. */
    private UUID criarContaDireto(UUID usuarioId, BigDecimal saldoInicial) {
        Conta conta = Conta.criar(usuarioId, "Conta de outro usuário", TipoConta.CORRENTE, saldoInicial, "Banco X");
        QuarkusTransaction.requiringNew().run(() -> contaRepository.salvar(conta));
        return conta.getId();
    }
}
