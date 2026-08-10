package br.com.wepdev.financas.budget.infrastructure.rest;

import br.com.wepdev.financas.budget.infrastructure.rest.dto.DefinirReservaRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.jwt.Claim;
import io.quarkus.test.security.jwt.JwtSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ReservaResourceTest {

    private static final String SUB_NUNCA_DEFINIU = "b0d9e700-0000-4000-8000-0000000000a1";
    private static final String SUB_DEFINE = "b0d9e700-0000-4000-8000-0000000000a2";

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_NUNCA_DEFINIU))
    void deveriaDevolverReservaZero_quandoUsuarioNuncaDefiniu() {
        given()
        .when()
                .get("/api/v1/reserva")
        .then()
                .statusCode(200)
                .body("valor", equalTo(0))
                .body("atualizadoEm", nullValue());
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_DEFINE))
    void deveriaDefinirEDepoisAtualizarReserva() {
        DefinirReservaRequest primeira = new DefinirReservaRequest(new BigDecimal("500.00"));

        given()
                .contentType(ContentType.JSON)
                .body(primeira)
        .when()
                .put("/api/v1/reserva")
        .then()
                .statusCode(200)
                .body("valor", equalTo(500.00f));

        DefinirReservaRequest segunda = new DefinirReservaRequest(new BigDecimal("700.00"));
        given()
                .contentType(ContentType.JSON)
                .body(segunda)
        .when()
                .put("/api/v1/reserva")
        .then()
                .statusCode(200)
                .body("valor", equalTo(700.00f));

        given()
        .when()
                .get("/api/v1/reserva")
        .then()
                .statusCode(200)
                .body("valor", equalTo(700.00f));
    }

    @Test
    @TestSecurity(user = "usuario-teste", roles = "usuario")
    @JwtSecurity(claims = @Claim(key = "sub", value = SUB_DEFINE))
    void deveriaRetornar400_quandoValorNegativo() {
        DefinirReservaRequest request = new DefinirReservaRequest(new BigDecimal("-1"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
        .when()
                .put("/api/v1/reserva")
        .then()
                .statusCode(400);
    }

    @Test
    void deveriaRetornar401_quandoSemAutenticacao() {
        given()
        .when()
                .get("/api/v1/reserva")
        .then()
                .statusCode(401);
    }
}
