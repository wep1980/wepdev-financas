package br.com.wepdev.financas.transaction.infrastructure.rest;

import br.com.wepdev.financas.transaction.domain.FrequenciaRecorrencia;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrente;
import br.com.wepdev.financas.transaction.domain.TransacaoRecorrenteRepository;
import br.com.wepdev.financas.transaction.infrastructure.client.AccountServiceClientImpl;
import br.com.wepdev.financas.transaction.infrastructure.rest.dto.CriarTransacaoRecorrenteRequest;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@QuarkusTest
class TransacaoRecorrenteResourceTest {

    private static final String SUB_USUARIO_TESTE = "b20c1000-0000-4000-8000-000000000010";
    private static final String SUB_LISTAGEM = "b20c1000-0000-4000-8000-000000000011";
    // sub exclusivo pro teste de isolamento — a asserção é "size() == 0",
    // não pode compartilhar usuário com testes que criam regra pra
    // SUB_USUARIO_TESTE (sem rollback entre métodos na mesma classe).
    private static final String SUB_ISOLAMENTO = "b20c1000-0000-4000-8000-000000000012";

    @Inject
    TransacaoRecorrenteRepository transacaoRecorrenteRepository;

    private AccountServiceClientImpl accountServiceClientMock;

    @BeforeEach
    void setUp() {
        accountServiceClientMock = mock(AccountServiceClientImpl.class);
        QuarkusMock.installMockForType(accountServiceClientMock, AccountServiceClientImpl.class);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCriarRegra_eGerarPrimeiraOcorrencia() {
        UUID contaId = UUID.randomUUID();
        CriarTransacaoRecorrenteRequest request = new CriarTransacaoRecorrenteRequest(
                contaId, "Salário", new BigDecimal("5000.00"), TipoTransacao.RECEITA, "Salário",
                FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 1, 15), null
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("status", equalTo("ATIVA"))
                .body("ocorrenciasGeradas", equalTo(1));

        verify(accountServiceClientMock).creditar(contaId, new BigDecimal("5000.00"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaNascerConcluida_quandoQuantidadeOcorrenciasEhUm() {
        CriarTransacaoRecorrenteRequest request = new CriarTransacaoRecorrenteRequest(
                UUID.randomUUID(), "Pagamento único", new BigDecimal("100.00"), TipoTransacao.DESPESA, "Outros",
                FrequenciaRecorrencia.MENSAL, LocalDate.of(2026, 1, 15), 1
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .post("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(201)
                .body("status", equalTo("CONCLUIDA"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar400_quandoCamposObrigatoriosAusentes() {
        String jsonInvalido = """
                {"valor": 100.00, "tipo": "DESPESA"}
                """;

        given()
                .contentType(ContentType.JSON)
                .body(jsonInvalido)
        .when()
                .post("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(400);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_LISTAGEM))
    void deveriaListarRegrasDoUsuarioAutenticado_eFiltrarPorStatus() {
        criarRegraDireto(UUID.fromString(SUB_LISTAGEM), "Salário", TipoTransacao.RECEITA);

        given()
        .when()
                .get("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(1));

        given()
                .queryParam("status", "CANCELADA")
        .when()
                .get("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_ISOLAMENTO))
    void naoDeveriaListarRegraDeOutroUsuario() {
        criarRegraDireto(UUID.randomUUID(), "Salário de outro usuário", TipoTransacao.RECEITA);

        given()
        .when()
                .get("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaBuscarRegraPorId() {
        UUID id = criarRegraDireto(UUID.fromString(SUB_USUARIO_TESTE), "Aluguel", TipoTransacao.DESPESA);

        given()
        .when()
                .get("/api/v1/transacoes-recorrentes/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id.toString()));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoBuscarRegraInexistente() {
        given()
        .when()
                .get("/api/v1/transacoes-recorrentes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoBuscarRegraDeOutroUsuario() {
        UUID id = criarRegraDireto(UUID.randomUUID(), "Aluguel de outro usuário", TipoTransacao.DESPESA);

        given()
        .when()
                .get("/api/v1/transacoes-recorrentes/{id}", id)
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaCancelarRegra_eSerIdempotente() {
        UUID id = criarRegraDireto(UUID.fromString(SUB_USUARIO_TESTE), "Assinatura", TipoTransacao.DESPESA);

        given().when().delete("/api/v1/transacoes-recorrentes/{id}", id).then().statusCode(204);
        given().when().delete("/api/v1/transacoes-recorrentes/{id}", id).then().statusCode(204);

        given()
        .when()
                .get("/api/v1/transacoes-recorrentes/{id}", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("CANCELADA"));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_USUARIO_TESTE))
    void deveriaRetornar404_quandoCancelarRegraInexistente() {
        given()
        .when()
                .delete("/api/v1/transacoes-recorrentes/{id}", UUID.randomUUID())
        .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "servico-teste", roles = "service")
    void deveriaListarProximosVencimentos_comRoleService() {
        criarRegraDireto(UUID.randomUUID(), "Aluguel", TipoTransacao.DESPESA);

        given()
                .queryParam("dias", 60)
        .when()
                .get("/api/v1/transacoes-recorrentes/proximos-vencimentos")
        .then()
                .statusCode(200);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
        .when()
                .get("/api/v1/transacoes-recorrentes")
        .then()
                .statusCode(401);
    }

    /** Insere direto no repositório, com dataInicio = hoje, pra aparecer nos próximos vencimentos independente da data em que o teste roda. */
    private UUID criarRegraDireto(UUID usuarioId, String descricao, TipoTransacao tipo) {
        TransacaoRecorrente regra = TransacaoRecorrente.criar(UUID.randomUUID(), usuarioId, descricao,
                new BigDecimal("100.00"), tipo, "Categoria", FrequenciaRecorrencia.MENSAL, LocalDate.now(), null);
        QuarkusTransaction.requiringNew().run(() -> transacaoRecorrenteRepository.salvar(regra));
        return regra.getId();
    }
}
