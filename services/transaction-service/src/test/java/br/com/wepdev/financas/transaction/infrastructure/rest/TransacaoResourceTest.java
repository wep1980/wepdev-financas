package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.transaction.domain.SaldoInsuficienteException;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.AtualizarTransacaoRequest;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.CriarTransacaoRequest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * O account-service real não roda nesse teste — o port AccountServiceClient
 * é substituído por um mock via QuarkusMock, o que permite testar os
 * cenários de sucesso/falha (404, 422) sem depender de rede nem de um
 * account-service de verdade no ar (equivalente ao que a task pedia como
 * "WireMock ou stub").
 */
@QuarkusTest
class TransacaoResourceTest {

    private static final String SUB_USUARIO_TESTE = "b20c1000-0000-4000-8000-000000000001";
    // subs exclusivos dos testes de listagem — precisam de contagem exata,
    // não podem compartilhar usuário com os demais testes da classe (mesma
    // base entre métodos, sem rollback automático).
    private static final String SUB_LISTAGEM_1 = "b20c1000-0000-4000-8000-000000000002";
    private static final String SUB_LISTAGEM_2 = "b20c1000-0000-4000-8000-000000000003";

    @Inject
    TransacaoRepository transacaoRepository;

    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRegistrarDespesa_quandoContaValida() {
        UUID contaId = UUID.randomUUID();
        CriarTransacaoRequest request = new CriarTransacaoRequest(
                contaId, "Mercado", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Alimentação", null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("contaId", equalTo(contaId.toString()))
                .body("usuarioId", equalTo(SUB_USUARIO_TESTE))
                .body("status", equalTo("CONFIRMADA"))
                .body("tipo", equalTo("DESPESA"));

        verify(accountServiceClientMock).debitar(eq(contaId), eq(new BigDecimal("100.00")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRegistrarReceita_quandoContaValida() {
        UUID contaId = UUID.randomUUID();
        CriarTransacaoRequest request = new CriarTransacaoRequest(
                contaId, "Salário", new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário", null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(201)
                .body("tipo", equalTo("RECEITA"));

        verify(accountServiceClientMock).creditar(eq(contaId), eq(new BigDecimal("5000.00")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoSaldoInsuficiente() {
        UUID contaId = UUID.randomUUID();
        doThrow(new SaldoInsuficienteException(contaId)).when(accountServiceClientMock).debitar(any(), any());

        CriarTransacaoRequest request = new CriarTransacaoRequest(
                contaId, "Mercado", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Alimentação", null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(422)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoContaNaoEncontradaOuDeOutroUsuario() {
        UUID contaId = UUID.randomUUID();
        doThrow(new ContaNaoEncontradaException(contaId)).when(accountServiceClientMock).debitar(any(), any());

        CriarTransacaoRequest request = new CriarTransacaoRequest(
                contaId, "Mercado", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Alimentação", null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoDescricaoAusente() {
        String jsonInvalido = """
                {"contaId": "%s", "valor": 100.00, "tipo": "DESPESA"}
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("descricao"));
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        CriarTransacaoRequest request = new CriarTransacaoRequest(
                UUID.randomUUID(), "Mercado", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Alimentação", null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM_1))
    void deveriaListarTransacoesDoUsuarioAutenticado_eFiltrarPorContaId() {
        UUID contaA = UUID.randomUUID();
        UUID contaB = UUID.randomUUID();
        registrarTransacao(contaA, "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));
        registrarTransacao(contaB, "Salário", TipoTransacao.RECEITA, new BigDecimal("500.00"));

        given()
        .when()
                .get("/api/v1/transacoes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(2));

        given()
                .queryParam("contaId", contaA)
        .when()
                .get("/api/v1/transacoes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].contaId", equalTo(contaA.toString()));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM_2))
    void naoDeveriaListarTransacaoDeOutroUsuario() {
        given()
        .when()
                .get("/api/v1/transacoes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaAtualizarTransacao_eDebitarDelta_quandoValorAumenta() {
        UUID contaId = UUID.randomUUID();
        String id = registrarTransacaoEObterId(contaId, "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));

        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado maior", new BigDecimal("150.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(200)
                .body("descricao", equalTo("Mercado maior"))
                .body("valor", equalTo(150.00f));

        verify(accountServiceClientMock).debitar(eq(contaId), eq(new BigDecimal("50.00")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaAtualizarTransacao_eCreditarDelta_quandoValorDiminui() {
        UUID contaId = UUID.randomUUID();
        String id = registrarTransacaoEObterId(contaId, "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));

        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado menor", new BigDecimal("60.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(200)
                .body("valor", equalTo(60.00f));

        verify(accountServiceClientMock).creditar(eq(contaId), eq(new BigDecimal("40.00")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void naoDeveriaChamarAccountService_quandoAtualizarSemMudarValor() {
        UUID contaId = UUID.randomUUID();
        String id = registrarTransacaoEObterId(contaId, "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));

        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado renomeado", new BigDecimal("100.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(200)
                .body("descricao", equalTo("Mercado renomeado"));

        verify(accountServiceClientMock, times(0)).creditar(any(), any());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoAtualizarTransacaoInexistente() {
        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado", new BigDecimal("100.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoAtualizarTransacaoDeOutroUsuario() {
        UUID id = criarTransacaoDireto(UUID.randomUUID());
        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado", new BigDecimal("100.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoAtualizarTransacaoCancelada() {
        String id = registrarTransacaoEObterId(UUID.randomUUID(), "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));
        given().when().delete("/api/v1/transacoes/{id}", id).then().statusCode(204);

        AtualizarTransacaoRequest request = new AtualizarTransacaoRequest("Mercado", new BigDecimal("150.00"), "Alimentação", null);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(422)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoAtualizarComDescricaoAusente() {
        String id = registrarTransacaoEObterId(UUID.randomUUID(), "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));
        String jsonInvalido = """
                {"valor": 100.00}
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .put("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("descricao"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCancelarTransacao_eReverterSaldo_quandoEraDespesa() {
        String id = registrarTransacaoEObterId(UUID.randomUUID(), "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));

        given()
        .when()
                .delete("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(204);

        verify(accountServiceClientMock).creditar(any(), eq(new BigDecimal("100.00")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaSerIdempotente_quandoCancelarDuasVezes() {
        String id = registrarTransacaoEObterId(UUID.randomUUID(), "Mercado", TipoTransacao.DESPESA, new BigDecimal("100.00"));

        given().when().delete("/api/v1/transacoes/{id}", id).then().statusCode(204);
        given().when().delete("/api/v1/transacoes/{id}", id).then().statusCode(204);

        verify(accountServiceClientMock, times(1)).creditar(any(), any());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoCancelarTransacaoInexistente() {
        given()
        .when()
                .delete("/api/v1/transacoes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoCancelarTransacaoDeOutroUsuario() {
        UUID id = criarTransacaoDireto(UUID.randomUUID());

        given()
        .when()
                .delete("/api/v1/transacoes/{id}", id)
        .then()
                .statusCode(404);
    }

    private void registrarTransacao(UUID contaId, String descricao, TipoTransacao tipo, BigDecimal valor) {
        registrarTransacaoEObterId(contaId, descricao, tipo, valor);
    }

    private String registrarTransacaoEObterId(UUID contaId, String descricao, TipoTransacao tipo, BigDecimal valor) {
        CriarTransacaoRequest request = new CriarTransacaoRequest(contaId, descricao, valor, tipo, "Categoria", null);
        return given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    /** Insere direto no repositório, fora do fluxo REST, pra simular transação de outro usuário sem depender de outro token real. */
    private UUID criarTransacaoDireto(UUID usuarioId) {
        Transacao transacao = Transacao.criar(UUID.randomUUID(), usuarioId, "De outro usuário",
                new BigDecimal("10.00"), TipoTransacao.DESPESA, "Categoria", null);
        QuarkusTransaction.requiringNew().run(() -> transacaoRepository.salvar(transacao));
        return transacao.getId();
    }
}
