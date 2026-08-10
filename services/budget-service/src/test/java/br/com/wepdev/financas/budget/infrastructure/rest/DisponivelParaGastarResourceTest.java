package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.domain.Conta;
import br.com.wepdev.financas.budget.domain.DespesaRecorrente;
import br.com.wepdev.financas.budget.domain.FaturaFechada;
import br.com.wepdev.financas.budget.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.budget.infrastructure.client.CardServiceClientImpl;
import br.com.wepdev.financas.budget.infrastructure.client.TransactionServiceClientImpl;
import br.com.wepdev.financas.budget.infrastructure.rest.dto.DefinirReservaRequest;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * account-service/card-service/transaction-service reais não rodam nesse
 * teste — os três ports são substituídos por mock via QuarkusMock, mesmo
 * padrão do AccountServiceClientImpl em CartaoResourceTest.
 */
@QuarkusTest
class DisponivelParaGastarResourceTest {

    private static final String SUB_USUARIO_TESTE = "b0d9e700-0000-4000-8000-0000000000b1";
    private static final String SUB_SEM_RESERVA = "b0d9e700-0000-4000-8000-0000000000b2";

    private AccountServiceClientImpl accountServiceClientMock;
    private CardServiceClientImpl cardServiceClientMock;
    private TransactionServiceClientImpl transactionServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        cardServiceClientMock = mock(CardServiceClientImpl.class);
        transactionServiceClientMock = mock(TransactionServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(cardServiceClientMock, CardServiceClientImpl.class);
        QuarkusMock.installMockForType(transactionServiceClientMock, TransactionServiceClientImpl.class);

        when(accountServiceClientMock.buscarContasAtivas()).thenReturn(List.of());
        when(cardServiceClientMock.buscarFaturasFechadas()).thenReturn(List.of());
        when(transactionServiceClientMock.buscarDespesasRecorrentesAtivas()).thenReturn(List.of());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCalcularDisponivel_comDetalhamentoCompleto() {
        when(accountServiceClientMock.buscarContasAtivas()).thenReturn(List.of(
                new Conta(UUID.randomUUID(), "Corrente", "CORRENTE", new BigDecimal("3000.00")),
                new Conta(UUID.randomUUID(), "Poupança", "POUPANCA", new BigDecimal("10000.00"))
        ));
        when(cardServiceClientMock.buscarFaturasFechadas()).thenReturn(List.of(
                new FaturaFechada(UUID.randomUUID(), "Nubank", new BigDecimal("500.00"), LocalDate.of(2026, 8, 10))
        ));
        when(transactionServiceClientMock.buscarDespesasRecorrentesAtivas()).thenReturn(List.of(
                new DespesaRecorrente(UUID.randomUUID(), "Aluguel", new BigDecimal("1500.00"), LocalDate.of(2026, 1, 1))
        ));
        DefinirReservaRequest reserva = new DefinirReservaRequest(new BigDecimal("200.00"));
        given().contentType(ContentType.JSON).body(reserva).when().put("/api/v1/reserva").then().statusCode(200);

        given()
                .queryParam("mes", "2026-08")
        .when()
                .get("/api/v1/disponivel-para-gastar")
        .then()
                .statusCode(200)
                .body("mesReferencia", equalTo("2026-08"))
                .body("saldoContas", equalTo(3000.00f))
                .body("faturasEmAberto", equalTo(500.00f))
                .body("despesasRecorrentes", equalTo(1500.00f))
                .body("reserva", equalTo(200.00f))
                .body("valorDisponivel", equalTo(800.00f))
                .body("detalhamento.contas.size()", equalTo(1))
                .body("detalhamento.faturas.size()", equalTo(1))
                .body("detalhamento.despesasRecorrentes.size()", equalTo(1));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_SEM_RESERVA))
    void deveriaUsarReservaZero_quandoUsuarioNuncaDefiniu() {
        given()
                .queryParam("mes", "2026-08")
        .when()
                .get("/api/v1/disponivel-para-gastar")
        .then()
                .statusCode(200)
                .body("reserva", equalTo(0))
                .body("valorDisponivel", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoParametroMesAusenteOuInvalido() {
        given().when().get("/api/v1/disponivel-para-gastar").then().statusCode(400);
        given().queryParam("mes", "2026-13").when().get("/api/v1/disponivel-para-gastar").then().statusCode(400);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
                .queryParam("mes", "2026-08")
        .when()
                .get("/api/v1/disponivel-para-gastar")
        .then()
                .statusCode(401);
    }
}
