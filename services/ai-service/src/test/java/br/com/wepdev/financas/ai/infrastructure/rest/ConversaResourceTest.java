package br.com.wepdev.financas.ai.infrastructure.rest;

import br.com.wepdev.financas.ai.domain.ChatResponse;
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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cria a conversa de teste via POST /chat de verdade (não há endpoint pra criar direto) — mesmo mock de LLM do ChatResourceTest. */
@QuarkusTest
class ConversaResourceTest {

    private static final String SUB_USUARIO_TESTE = "a1500000-0000-4000-8000-000000000002";
    private static final String SUB_ISOLAMENTO = "a1500000-0000-4000-8000-000000000003";

    private LlmProvider llmProviderMock;

    @BeforeEach
    void setUp() {
        LlmProviderFactoryImpl llmProviderFactoryMock = mock(LlmProviderFactoryImpl.class);
        llmProviderMock = mock(LlmProvider.class);
        when(llmProviderFactoryMock.criar(any())).thenReturn(llmProviderMock);
        QuarkusMock.installMockForType(llmProviderFactoryMock, LlmProviderFactoryImpl.class);

        QuarkusMock.installMockForType(mock(AccountServiceClientImpl.class), AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(BudgetServiceClientImpl.class), BudgetServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(CardServiceClientImpl.class), CardServiceClientImpl.class);
        QuarkusMock.installMockForType(mock(TransactionServiceClientImpl.class), TransactionServiceClientImpl.class);

        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse("{\"intent\": \"DESCONHECIDA\"}"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaListarConversaCriada() {
        criarConversaEObterId("oi tudo bem?");

        given()
        .when()
                .get("/api/v1/conversas")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].ultimaMensagemPreview", notNullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaBuscarConversaComHistoricoDeMensagens() {
        String id = criarConversaEObterId("primeira mensagem");

        given()
        .when()
                .get("/api/v1/conversas/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("mensagens.size()", equalTo(2))
                .body("mensagens[0].autor", equalTo("USUARIO"))
                .body("mensagens[1].autor", equalTo("AGENTE"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoConversaInexistente() {
        given()
        .when()
                .get("/api/v1/conversas/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ISOLAMENTO))
    void naoDeveriaListarConversaDeOutroUsuario() {
        given()
        .when()
                .get("/api/v1/conversas")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    private String criarConversaEObterId(String mensagem) {
        return given()
                .contentType(ContentType.JSON)
                .body(new ChatRequest(null, mensagem))
        .when()
                .post("/api/v1/chat")
        .then()
                .statusCode(200)
                .extract().path("conversaId");
    }
}
