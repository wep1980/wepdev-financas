package br.com.wepdev.financas.document.infrastructure.rest;

import br.com.wepdev.financas.document.domain.ChatResponse;
import br.com.wepdev.financas.document.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.document.infrastructure.llm.OllamaLlmProvider;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ConfirmarLancamentosRequest;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O Ollama real não roda nesse teste — a porta {@link LlmProvider} é
 * substituída por um mock via QuarkusMock (mesmo padrão do
 * AccountServiceClientImpl no card-service). Processamento é assíncrono
 * (ADR-0024), então os testes que dependem do resultado da extração usam
 * Awaitility pra esperar o job em background terminar, sem sleep arbitrário.
 */
@QuarkusTest
class DocumentoResourceTest {

    private static final String SUB_USUARIO_TESTE = "d0c00000-0000-4000-8000-000000000001";
    private static final String SUB_LISTAGEM = "d0c00000-0000-4000-8000-000000000002";
    private static final String SUB_ISOLAMENTO = "d0c00000-0000-4000-8000-000000000003";

    private OllamaLlmProvider llmProviderMock;
    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        llmProviderMock = mock(OllamaLlmProvider.class);
        QuarkusMock.installMockForType(llmProviderMock, OllamaLlmProvider.class);
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaAceitarUpload_eRetornar202ComStatusRecebido() throws IOException {
        given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .body("status", equalTo("RECEBIDO"))
                .body("usuarioId", equalTo(SUB_USUARIO_TESTE))
                .body("lancamentos", hasSize(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaFicarAguardandoConfirmacao_quandoLlmExtraiLancamentos() throws IOException {
        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": [
                  {"descricao": "Mercado", "valor": "50.00", "dataTexto": "05/08/2026", "tipo": "DESPESA", "categoriaSugerida": "Alimentação"}
                ]}
                """));

        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                .when()
                        .get("/api/v1/documentos/{id}", documentoId)
                .then()
                        .statusCode(200)
                        .body("status", equalTo("AGUARDANDO_CONFIRMACAO"))
                        .body("lancamentos", hasSize(1))
                        .body("lancamentos[0].descricao", equalTo("Mercado")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaMarcarErro_quandoLlmNaoExtraiNada() throws IOException {
        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": []}
                """));

        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given()
                .when()
                        .get("/api/v1/documentos/{id}", documentoId)
                .then()
                        .statusCode(200)
                        .body("status", equalTo("ERRO_PROCESSAMENTO"))
                        .body("mensagemErro", equalTo("Nenhum lançamento reconhecido no documento")));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoArquivoAusente() {
        given()
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoTipoInvalido() throws IOException {
        given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "ALGO_ESQUISITO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(400);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
        .when()
                .get("/api/v1/documentos")
        .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM))
    void deveriaListarDocumentosDoUsuarioAutenticado() throws IOException {
        given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202);

        given()
        .when()
                .get("/api/v1/documentos")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].usuarioId", equalTo(SUB_LISTAGEM));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ISOLAMENTO))
    void naoDeveriaListarDocumentoDeOutroUsuario() {
        given()
        .when()
                .get("/api/v1/documentos")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoDocumentoInexistente() {
        given()
        .when()
                .get("/api/v1/documentos/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaConfirmarLancamentos_eSerIdempotente() throws IOException {
        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": [
                  {"descricao": "Mercado", "valor": "50.00", "dataTexto": "05/08/2026", "tipo": "DESPESA", "categoriaSugerida": null}
                ]}
                """));
        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given().when().get("/api/v1/documentos/{id}", documentoId)
                        .then().statusCode(200).body("status", equalTo("AGUARDANDO_CONFIRMACAO")));

        String lancamentoId = given().when().get("/api/v1/documentos/{id}", documentoId)
                .then().extract().path("lancamentos[0].id");
        var request = new ConfirmarLancamentosRequest(UUID.randomUUID(), Set.of(UUID.fromString(lancamentoId)));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/documentos/{id}/confirmar", documentoId)
        .then()
                .statusCode(204);

        given()
        .when()
                .get("/api/v1/documentos/{id}", documentoId)
        .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"))
                .body("lancamentos[0].status", equalTo("CONFIRMADO"));

        // idempotente — confirmar de novo continua 204
        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/documentos/{id}/confirmar", documentoId)
        .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoDocumentoAindaNaoProcessado() throws IOException {
        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        var request = new ConfirmarLancamentosRequest(UUID.randomUUID(), Set.of(UUID.randomUUID()));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/documentos/{id}/confirmar", documentoId)
        .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar422_quandoNenhumLancamentoSelecionado() throws IOException {
        when(llmProviderMock.chat(any())).thenReturn(new ChatResponse("""
                {"anoReferencia": "2026", "lancamentos": [
                  {"descricao": "Mercado", "valor": "50.00", "dataTexto": "05/08/2026", "tipo": "DESPESA", "categoriaSugerida": null}
                ]}
                """));
        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                given().when().get("/api/v1/documentos/{id}", documentoId)
                        .then().statusCode(200).body("status", equalTo("AGUARDANDO_CONFIRMACAO")));

        var request = new ConfirmarLancamentosRequest(UUID.randomUUID(), Set.of());

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/documentos/{id}/confirmar", documentoId)
        .then()
                .statusCode(422);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_aoConfirmarDocumentoInexistente() {
        var request = new ConfirmarLancamentosRequest(UUID.randomUUID(), Set.of(UUID.randomUUID()));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/documentos/{id}/confirmar", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoContaIdAusente() throws IOException {
        String documentoId = given()
                .multiPart("arquivo", "fatura.pdf", gerarPdfDeTeste(), "application/pdf")
                .multiPart("tipo", "FATURA_CARTAO")
        .when()
                .post("/api/v1/documentos")
        .then()
                .statusCode(202)
                .extract().path("id");

        given()
                .contentType(ContentType.JSON)
                .body("{\"lancamentoIdsConfirmados\": [\"" + UUID.randomUUID() + "\"]}")
        .when()
                .post("/api/v1/documentos/{id}/confirmar", documentoId)
        .then()
                .statusCode(400);
    }

    private byte[] gerarPdfDeTeste() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.beginText();
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                conteudo.newLineAtOffset(50, 700);
                conteudo.showText("Fatura de teste - Supermercado 150.00");
                conteudo.endText();
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }
}
