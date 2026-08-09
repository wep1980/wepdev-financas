package br.com.wepdev.financas.card.infrastructure.rest;

import br.com.wepdev.financas.card.domain.Bandeira;
import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.infrastructure.client.AccountServiceClientImpl;
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
import java.time.YearMonth;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
class FaturaResourceTest {

    private static final String SUB_USUARIO_TESTE = "c30c1000-0000-4000-8000-000000000010";

    @Inject
    CartaoRepository cartaoRepository;

    @Inject
    FaturaRepository faturaRepository;

    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaBuscarFaturaComParcelas() {
        String cartaoId = criarCartaoEObterId();

        LancarCompraRequest compra = new LancarCompraRequest(
                "Mercado", new BigDecimal("50.00"), "Alimentação", LocalDate.of(2026, 8, 1), 1
        );
        String faturaId = given()
                .contentType(ContentType.JSON)
                .body(compra)
        .when()
                .post("/api/v1/cartoes/{id}/compras", cartaoId)
        .then()
                .statusCode(201)
                .extract().path("parcelas[0].faturaId");

        given()
        .when()
                .get("/api/v1/faturas/{id}", faturaId)
        .then()
                .statusCode(200)
                .body("id", equalTo(faturaId))
                .body("status", equalTo("ABERTA"))
                .body("valorTotal", equalTo(50.00f))
                .body("parcelas.size()", equalTo(1))
                .body("parcelas[0].descricao", equalTo("Mercado"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoFaturaInexistente() {
        given()
        .when()
                .get("/api/v1/faturas/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoFaturaDeOutroUsuario() {
        UUID outroUsuario = UUID.randomUUID();
        UUID cartaoId = criarCartaoDireto(outroUsuario);
        Fatura fatura = Fatura.criar(cartaoId, outroUsuario, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 5),
                LocalDate.of(2026, 8, 12));
        QuarkusTransaction.requiringNew().run(() -> faturaRepository.salvar(fatura));

        given()
        .when()
                .get("/api/v1/faturas/{id}", fatura.getId())
        .then()
                .statusCode(404);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
        .when()
                .get("/api/v1/faturas/{id}", UUID.randomUUID())
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "servico-teste", roles = "service")
    void deveriaListarProximosVencimentos_comRoleService() {
        UUID usuarioId = UUID.randomUUID();
        UUID cartaoId = criarCartaoDireto(usuarioId);
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, YearMonth.from(LocalDate.now()), LocalDate.now(), LocalDate.now());
        fatura.adicionarParcela(new BigDecimal("100.00"));
        fatura.fechar();
        QuarkusTransaction.requiringNew().run(() -> faturaRepository.salvar(fatura));

        given()
                .queryParam("dias", 30)
        .when()
                .get("/api/v1/faturas/proximos-vencimentos")
        .then()
                .statusCode(200)
                .body("faturaId", org.hamcrest.Matchers.hasItem(fatura.getId().toString()));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar403_quandoProximosVencimentosComRoleUsuario() {
        given()
                .queryParam("dias", 30)
        .when()
                .get("/api/v1/faturas/proximos-vencimentos")
        .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaPagarFatura_eSerIdempotente() {
        UUID usuarioId = UUID.fromString(SUB_USUARIO_TESTE);
        String cartaoId = criarCartaoEObterId();
        UUID faturaId = criarFaturaFechadaDireto(UUID.fromString(cartaoId), usuarioId, new BigDecimal("150.00"));

        given().when().post("/api/v1/faturas/{id}/pagar", faturaId).then().statusCode(204);
        verify(accountServiceClientMock, times(1)).debitar(any(), eq(new BigDecimal("150.00")));

        given().when().post("/api/v1/faturas/{id}/pagar", faturaId).then().statusCode(204);
        verify(accountServiceClientMock, times(1)).debitar(any(), any());

        given()
        .when()
                .get("/api/v1/faturas/{id}", faturaId)
        .then()
                .statusCode(200)
                .body("status", equalTo("PAGA"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoFaturaAindaAberta() {
        String cartaoId = criarCartaoEObterId();
        LancarCompraRequest compra = new LancarCompraRequest(
                "Mercado", new BigDecimal("50.00"), "Alimentação", LocalDate.of(2026, 8, 1), 1
        );
        String faturaId = given()
                .contentType(ContentType.JSON)
                .body(compra)
        .when()
                .post("/api/v1/cartoes/{id}/compras", cartaoId)
        .then()
                .statusCode(201)
                .extract().path("parcelas[0].faturaId");

        given()
        .when()
                .post("/api/v1/faturas/{id}/pagar", faturaId)
        .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoPagarFaturaInexistente() {
        given()
        .when()
                .post("/api/v1/faturas/{id}/pagar", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    private UUID criarFaturaFechadaDireto(UUID cartaoId, UUID usuarioId, BigDecimal valorTotal) {
        Fatura fatura = Fatura.criar(cartaoId, usuarioId, YearMonth.of(2026, 8), LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 20));
        fatura.adicionarParcela(valorTotal);
        fatura.fechar();
        QuarkusTransaction.requiringNew().run(() -> faturaRepository.salvar(fatura));
        return fatura.getId();
    }

    private String criarCartaoEObterId() {
        CriarCartaoRequest request = new CriarCartaoRequest(
                "Nubank", Bandeira.VISA, new BigDecimal("5000.00"), 10, 20, UUID.randomUUID()
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

    private UUID criarCartaoDireto(UUID usuarioId) {
        Cartao cartao = Cartao.criar(usuarioId, "De outro usuário", Bandeira.VISA, new BigDecimal("100.00"),
                5, 12, UUID.randomUUID());
        QuarkusTransaction.requiringNew().run(() -> cartaoRepository.salvar(cartao));
        return cartao.getId();
    }
}
