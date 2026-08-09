package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.card.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.card.infrastructure.rest.dto.AtualizarCartaoRequest;
import br.com.wepdev.financas.card.infrastructure.rest.dto.CriarCartaoRequest;
import br.com.wepdev.financas.card.infrastructure.rest.dto.LancarCompraRequest;
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
import java.time.LocalDate;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * O account-service real não roda nesse teste — o port AccountServiceClient
 * é substituído por um mock via QuarkusMock, mesmo padrão do
 * transaction-service (TransacaoResourceTest).
 */
@QuarkusTest
class CartaoResourceTest {

    private static final String SUB_USUARIO_TESTE = "c30c1000-0000-4000-8000-000000000001";
    // sub exclusivo pro teste de listagem — precisa de contagem exata, não
    // pode compartilhar usuário com os demais testes da classe (sem
    // rollback automático entre métodos).
    private static final String SUB_LISTAGEM = "c30c1000-0000-4000-8000-000000000002";
    // sub exclusivo pro teste de isolamento — a asserção é "size() == 0",
    // não pode compartilhar usuário com testes que criam cartão pra
    // SUB_USUARIO_TESTE (sem rollback entre métodos na mesma classe).
    private static final String SUB_ISOLAMENTO = "c30c1000-0000-4000-8000-000000000003";
    private static final String SUB_COMPRA = "c30c1000-0000-4000-8000-000000000004";

    @Inject
    CartaoRepository cartaoRepository;

    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCriarCartao_quandoRequisicaoValida() {
        UUID contaPagamentoId = UUID.randomUUID();
        CriarCartaoRequest request = new CriarCartaoRequest(
                "Nubank Roxinho", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, contaPagamentoId
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("usuarioId", equalTo(SUB_USUARIO_TESTE))
                .body("apelido", equalTo("Nubank Roxinho"))
                .body("ativo", equalTo(true));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoContaPagamentoNaoEncontrada() {
        UUID contaPagamentoId = UUID.randomUUID();
        doThrow(new ContaNaoEncontradaException(contaPagamentoId))
                .when(accountServiceClientMock).confirmarPosseDaConta(contaPagamentoId);

        CriarCartaoRequest request = new CriarCartaoRequest(
                "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, contaPagamentoId
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes")
        .then()
                .statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoCamposObrigatoriosAusentes() {
        String jsonInvalido = """
                {"limite": 100.00, "diaFechamento": 5, "diaVencimento": 12, "contaPagamentoId": "%s"}
                """.formatted(UUID.randomUUID());

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/cartoes")
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("apelido"));
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        CriarCartaoRequest request = new CriarCartaoRequest(
                "Nubank", Bandeira.MASTERCARD, new BigDecimal("5000.00"), 5, 12, UUID.randomUUID()
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes")
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM))
    void deveriaListarCartoesDoUsuarioAutenticado() {
        criarCartaoEObterId("Cartão 1");
        criarCartaoEObterId("Cartão 2");

        given()
        .when()
                .get("/api/v1/cartoes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ISOLAMENTO))
    void naoDeveriaListarCartaoDeOutroUsuario() {
        criarCartaoDireto(UUID.randomUUID());

        given()
        .when()
                .get("/api/v1/cartoes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaBuscarCartaoPorId() {
        String id = criarCartaoEObterId("Nubank");

        given()
        .when()
                .get("/api/v1/cartoes/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoBuscarCartaoInexistente() {
        given()
        .when()
                .get("/api/v1/cartoes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoBuscarCartaoDeOutroUsuario() {
        UUID id = criarCartaoDireto(UUID.randomUUID());

        given()
        .when()
                .get("/api/v1/cartoes/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaAtualizarCartao() {
        String id = criarCartaoEObterId("Nubank antigo");
        UUID novaContaPagamento = UUID.randomUUID();
        AtualizarCartaoRequest request = new AtualizarCartaoRequest(
                "Nubank novo", Bandeira.ELO, new BigDecimal("8000.00"), 10, 20, novaContaPagamento
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/cartoes/{id}", id)
        .then()
                .statusCode(200)
                .body("apelido", equalTo("Nubank novo"))
                .body("contaPagamentoId", equalTo(novaContaPagamento.toString()));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaExcluirCartao_eSerIdempotente() {
        String id = criarCartaoEObterId("Nubank");

        given().when().delete("/api/v1/cartoes/{id}", id).then().statusCode(204);
        given().when().delete("/api/v1/cartoes/{id}", id).then().statusCode(204);

        given()
        .when()
                .get("/api/v1/cartoes/{id}", id)
        .then()
                .statusCode(200)
                .body("ativo", equalTo(false));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoExcluirCartaoInexistente() {
        given()
        .when()
                .delete("/api/v1/cartoes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_COMPRA))
    void deveriaLancarCompraAVista_eCriarFatura() {
        String cartaoId = criarCartaoEObterId("Nubank compra");
        LancarCompraRequest request = new LancarCompraRequest(
                "Mercado", new BigDecimal("100.00"), "Alimentação", LocalDate.of(2026, 8, 1), 1
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes/{id}/compras", cartaoId)
        .then()
                .statusCode(201)
                .body("quantidadeParcelas", equalTo(1))
                .body("parcelas.size()", equalTo(1))
                .body("parcelas[0].valor", equalTo(100.00f));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_COMPRA))
    void deveriaLancarCompraParcelada_eDistribuirEmFaturasConsecutivas() {
        String cartaoId = criarCartaoEObterId("Nubank parcelado");
        LancarCompraRequest request = new LancarCompraRequest(
                "Notebook", new BigDecimal("300.00"), "Eletrônicos", LocalDate.of(2026, 8, 1), 3
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes/{id}/compras", cartaoId)
        .then()
                .statusCode(201)
                .body("parcelas.size()", equalTo(3))
                .body("parcelas[0].numeroParcela", equalTo(1))
                .body("parcelas[2].numeroParcela", equalTo(3));

        given()
        .when()
                .get("/api/v1/cartoes/{id}/faturas", cartaoId)
        .then()
                .statusCode(200)
                .body("size()", equalTo(3));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoLancarCompraEmCartaoInexistente() {
        LancarCompraRequest request = new LancarCompraRequest(
                "Mercado", new BigDecimal("100.00"), "Alimentação", LocalDate.of(2026, 8, 1), 1
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes/{id}/compras", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoLancarCompraSemDescricao() {
        String cartaoId = criarCartaoEObterId("Nubank validação");
        String jsonInvalido = """
                {"valorTotal": 100.00, "dataCompra": "2026-08-01"}
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/cartoes/{id}/compras", cartaoId)
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("descricao"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoListarFaturasDeCartaoInexistente() {
        given()
        .when()
                .get("/api/v1/cartoes/{id}/faturas", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    private String criarCartaoEObterId(String apelido) {
        CriarCartaoRequest request = new CriarCartaoRequest(
                apelido, Bandeira.VISA, new BigDecimal("1000.00"), 5, 12, UUID.randomUUID()
        );
        return given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/cartoes")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    /** Insere direto no repositório, fora do fluxo REST, pra simular cartão de outro usuário sem depender de outro token real. */
    private UUID criarCartaoDireto(UUID usuarioId) {
        Cartao cartao = Cartao.criar(usuarioId, "De outro usuário", Bandeira.VISA, new BigDecimal("100.00"),
                5, 12, UUID.randomUUID());
        QuarkusTransaction.requiringNew().run(() -> cartaoRepository.salvar(cartao));
        return cartao.getId();
    }
}
