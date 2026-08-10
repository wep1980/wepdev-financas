package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.domain.Orcamento;
import br.com.wepdev.financas.budget.domain.OrcamentoRepository;
import br.com.wepdev.financas.budget.domain.ResumoCategoria;
import br.com.wepdev.financas.budget.infrastructure.client.TransactionServiceClientImpl;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.AtualizarOrcamentoRequest;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.CriarOrcamentoRequest;
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
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O transaction-service real não roda nesse teste — o port
 * TransactionServiceClient é substituído por um mock via QuarkusMock,
 * mesmo padrão do AccountServiceClientImpl em CartaoResourceTest.
 */
@QuarkusTest
class OrcamentoResourceTest {

    private static final String SUB_USUARIO_TESTE = "b0d9e700-0000-4000-8000-000000000001";
    private static final String SUB_LISTAGEM = "b0d9e700-0000-4000-8000-000000000002";
    private static final String SUB_ISOLAMENTO = "b0d9e700-0000-4000-8000-000000000003";
    // subs exclusivos — sem rollback automático entre métodos na mesma
    // classe, então cada teste que cria orçamento "Mercado"/2026-08
    // precisa do seu próprio usuário (senão colide com a checagem de
    // duplicata, OrcamentoJaExisteException).
    private static final String SUB_ATUALIZAR = "b0d9e700-0000-4000-8000-000000000004";
    private static final String SUB_EXCLUIR = "b0d9e700-0000-4000-8000-000000000005";

    @Inject
    OrcamentoRepository orcamentoRepository;

    private TransactionServiceClientImpl transactionServiceClientMock;

    @BeforeEach
    void setUp() {
        transactionServiceClientMock = mock(TransactionServiceClientImpl.class);
        QuarkusMock.installMockForType(transactionServiceClientMock, TransactionServiceClientImpl.class);
        when(transactionServiceClientMock.buscarResumoPorCategoria(any(), any())).thenReturn(List.of());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCriarOrcamento_comValorConsumidoDoTransactionService() {
        when(transactionServiceClientMock.buscarResumoPorCategoria(any(), any()))
                .thenReturn(List.of(new ResumoCategoria("Mercado", new BigDecimal("150.00"))));
        CriarOrcamentoRequest request = new CriarOrcamentoRequest("Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/orcamentos")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("usuarioId", equalTo(SUB_USUARIO_TESTE))
                .body("categoria", equalTo("Mercado"))
                .body("valorLimite", equalTo(800.00f))
                .body("valorConsumido", equalTo(150.00f))
                .body("valorDisponivel", equalTo(650.00f))
                .body("status", equalTo("ATIVO"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoJaExisteOrcamentoAtivoParaCategoriaEMes() {
        CriarOrcamentoRequest request = new CriarOrcamentoRequest("Transporte", YearMonth.of(2026, 8), new BigDecimal("300.00"));
        given().contentType(ContentType.JSON).body(request).when().post("/api/v1/orcamentos").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/orcamentos")
        .then()
                .statusCode(422)
                .body("mensagem", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoCategoriaAusente() {
        String jsonInvalido = """
                {"mesReferencia": "2026-08", "valorLimite": 100.00}
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/orcamentos")
        .then()
                .statusCode(400)
                .body("erros[0].campo", equalTo("categoria"));
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        CriarOrcamentoRequest request = new CriarOrcamentoRequest("Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/orcamentos")
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM))
    void deveriaListarOrcamentosAtivosDoMes() {
        criarOrcamentoEObterId("Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));
        criarOrcamentoEObterId("Lazer", YearMonth.of(2026, 8), new BigDecimal("300.00"));
        criarOrcamentoEObterId("Transporte", YearMonth.of(2026, 9), new BigDecimal("200.00"));

        given()
                .queryParam("mes", "2026-08")
        .when()
                .get("/api/v1/orcamentos")
        .then()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoParametroMesAusenteOuInvalido() {
        given().when().get("/api/v1/orcamentos").then().statusCode(400);
        given().queryParam("mes", "agosto-2026").when().get("/api/v1/orcamentos").then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ISOLAMENTO))
    void naoDeveriaListarOrcamentoDeOutroUsuario() {
        criarOrcamentoDireto(UUID.randomUUID(), "Mercado", YearMonth.of(2026, 8));

        given()
                .queryParam("mes", "2026-08")
        .when()
                .get("/api/v1/orcamentos")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ATUALIZAR))
    void deveriaAtualizarLimiteDoOrcamento() {
        String id = criarOrcamentoEObterId("Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));
        AtualizarOrcamentoRequest request = new AtualizarOrcamentoRequest(new BigDecimal("1000.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/orcamentos/{id}", id)
        .then()
                .statusCode(200)
                .body("valorLimite", equalTo(1000.00f));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoAtualizarOrcamentoInexistente() {
        AtualizarOrcamentoRequest request = new AtualizarOrcamentoRequest(new BigDecimal("1000.00"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/orcamentos/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_EXCLUIR))
    void deveriaExcluirOrcamento_eSerIdempotente() {
        String id = criarOrcamentoEObterId("Mercado", YearMonth.of(2026, 8), new BigDecimal("800.00"));

        given().when().delete("/api/v1/orcamentos/{id}", id).then().statusCode(204);
        given().when().delete("/api/v1/orcamentos/{id}", id).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoExcluirOrcamentoInexistente() {
        given()
        .when()
                .delete("/api/v1/orcamentos/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    private String criarOrcamentoEObterId(String categoria, YearMonth mesReferencia, BigDecimal valorLimite) {
        CriarOrcamentoRequest request = new CriarOrcamentoRequest(categoria, mesReferencia, valorLimite);
        return given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/orcamentos")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    /** Insere direto no repositório, fora do fluxo REST, pra simular orçamento de outro usuário sem depender de outro token real. */
    private void criarOrcamentoDireto(UUID usuarioId, String categoria, YearMonth mesReferencia) {
        Orcamento orcamento = Orcamento.criar(usuarioId, categoria, mesReferencia, new BigDecimal("100.00"));
        QuarkusTransaction.requiringNew().run(() -> orcamentoRepository.salvar(orcamento));
    }
}
