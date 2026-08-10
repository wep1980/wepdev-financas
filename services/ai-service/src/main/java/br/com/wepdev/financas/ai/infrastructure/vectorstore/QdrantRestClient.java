package br.com.wepdev.financas.ai.infrastructure.vectorstore;

import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantBuscarRequestDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantBuscarResponseDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantCriarColecaoRequestDto;
import br.com.wepdev.financas.ai.infrastructure.vectorstore.dto.QdrantUpsertPontosRequestDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.temporal.ChronoUnit;

/** Fala direto com a API REST do Qdrant (sem client Java dedicado — mesmo critério de "REST client interface" já usado com todo o resto do sistema). */
@RegisterRestClient(configKey = "qdrant")
public interface QdrantRestClient {

    /** 200 se já existir, 404 se não — usado só pra decidir se cria a coleção no startup (QdrantColecaoInicializador). */
    @GET
    @Path("/collections/{nome}")
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    Object verificarColecao(@PathParam("nome") String nome);

    @PUT
    @Path("/collections/{nome}")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    void criarColecao(@PathParam("nome") String nome, QdrantCriarColecaoRequestDto request);

    @PUT
    @Path("/collections/{nome}/points")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    void upsertPontos(@PathParam("nome") String nome, @QueryParam("wait") boolean esperar, QdrantUpsertPontosRequestDto request);

    @POST
    @Path("/collections/{nome}/points/search")
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    QdrantBuscarResponseDto buscar(@PathParam("nome") String nome, QdrantBuscarRequestDto request);
}
