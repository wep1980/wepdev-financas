package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.domain.ChatResponse;
import br.com.wepdev.financas.ai.domain.DisponivelParaGastar;
import br.com.wepdev.financas.ai.domain.LlmProvider;
import br.com.wepdev.financas.ai.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.ai.infrastructure.client.BudgetServiceClientImpl;
import br.com.wepdev.financas.ai.infrastructure.client.CardServiceClientImpl;
import br.com.wepdev.financas.ai.infrastructure.client.TransactionServiceClientImpl;
import br.com.wepdev.financas.ai.infrastructure.llm.LlmProviderFactoryImpl;
import br.com.wepdev.financas.ai.infrastructure.rest.dto.ChatRequest;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * account-service/budget-service/card-service/transaction-service e o
 * LlmProviderFactory reais não rodam nesse teste — todos substituídos
 * por mock via QuarkusMock, mesmo padrão do AccountServiceClientImpl em
 * CartaoResourceTest (card-service).
 */
@QuarkusTest
class ChatResourceTest {

    private static final String SUB_USUARIO_TESTE = "a1500000-0000-4000-8000-000000000001";

    private LlmProviderFactoryImpl llmProviderFactoryMock;
    private LlmProvider llmProviderMock;
    private BudgetServiceClientImpl budgetServiceClientMock;

    @BeforeEach
    void setUp() {
        llmProviderFactoryMock = mock(LlmProviderFactoryImpl.class);
        llmProviderMock = mock(LlmProvider.class);
        when(llmProviderFactoryMock.criar(any())).thenReturn(llmProviderMock);
        QuarkusMock.installMockForType(llmProviderFactoryMock, LlmProviderFactoryImpl.class);

        budgetServiceClientMock = mock(BudgetServiceClientImpl.class);
        QuarkusMock.installMockForType(budgetServiceClientMock, BudgetServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(AccountServiceClientImpl.class), AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(CardServiceClientImpl.class), CardServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(TransactionServiceClientImpl.class), TransactionServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaResponderConsulta_eIniciarConversaNova() {
        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse(
                "{\"intent\": \"CONSULTA\", \"tool\": \"buscar_saldo_disponivel\", \"periodo\": null}"));
        when(budgetServiceClientMock.buscarDisponivelParaGastar(YearMonth.now())).thenReturn(
                new DisponivelParaGastar(new BigDecimal("800.00"), new BigDecimal("3000.00"),
                        new BigDecimal("500.00"), new BigDecimal("1500.00"), new BigDecimal("200.00")));

        given()
                .contentType(ContentType.JSON)
                .body(new ChatRequest(null, "quanto posso gastar esse mês?"))
        .when()
                .post("/api/v1/chat")
        .then()
                .statusCode(200)
                .body("conversaId", notNullValue())
                .body("tipo", equalTo("RESPOSTA"))
                .body("resposta", org.hamcrest.Matchers.containsString("800.00"))
                .body("trace.size()", equalTo(1));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoMensagemVazia() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"mensagem\": \"\"}")
        .when()
                .post("/api/v1/chat")
        .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoConversaIdNaoExiste() {
        given()
                .contentType(ContentType.JSON)
                .body(new ChatRequest(java.util.UUID.randomUUID(), "oi"))
        .when()
                .post("/api/v1/chat")
        .then()
                .statusCode(404);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
                .contentType(ContentType.JSON)
                .body(new ChatRequest(null, "oi"))
        .when()
                .post("/api/v1/chat")
        .then()
                .statusCode(401);
    }
}
