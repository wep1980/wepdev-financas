package br.com.wepdev.financas.document.infrastructure.rest;

import br.com.wepdev.financas.document.application.BuscarDocumentoUseCase;
import br.com.wepdev.financas.document.application.ConfirmarLancamentosCommand;
import br.com.wepdev.financas.document.application.ConfirmarLancamentosUseCase;
import br.com.wepdev.financas.document.application.ListarDocumentosUseCase;
import br.com.wepdev.financas.document.application.UploadDocumentoCommand;
import br.com.wepdev.financas.document.application.UploadDocumentoUseCase;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ConfirmarLancamentosRequest;
import br.com.wepdev.financas.document.infrastructure.rest.dto.DocumentoImportadoResponse;
import br.com.wepdev.financas.document.infrastructure.rest.dto.ErroResponse;
import br.com.wepdev.financas.document.infrastructure.rest.dto.UploadDocumentoForm;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/** Conforme docs/specs/document-service.yaml — spec-driven, contrato antes do código. */
@Path("/api/v1/documentos")
public class DocumentoResource {

    private final UploadDocumentoUseCase uploadUseCase;
    private final ListarDocumentosUseCase listarUseCase;
    private final BuscarDocumentoUseCase buscarUseCase;
    private final ConfirmarLancamentosUseCase confirmarUseCase;
    private final SecurityIdentity identity;

    public DocumentoResource(UploadDocumentoUseCase uploadUseCase, ListarDocumentosUseCase listarUseCase,
                              BuscarDocumentoUseCase buscarUseCase, ConfirmarLancamentosUseCase confirmarUseCase,
                              SecurityIdentity identity) {
        this.uploadUseCase = uploadUseCase;
        this.listarUseCase = listarUseCase;
        this.buscarUseCase = buscarUseCase;
        this.confirmarUseCase = confirmarUseCase;
        this.identity = identity;
    }

    @POST
    @RolesAllowed("usuario")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@BeanParam UploadDocumentoForm form) {
        if (form.arquivo == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErroResponse("arquivo é obrigatório"))
                    .build();
        }
        TipoDocumento tipo;
        try {
            tipo = TipoDocumento.valueOf(form.tipo);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErroResponse("tipo inválido: " + form.tipo))
                    .build();
        }

        byte[] conteudo;
        try {
            conteudo = Files.readAllBytes(form.arquivo.filePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        DocumentoImportado documento = uploadUseCase.executar(new UploadDocumentoCommand(
                usuarioIdAutenticado(), tipo, form.arquivo.fileName(), conteudo, form.senha, form.nomeFiltro));

        return Response.status(Response.Status.ACCEPTED)
                .entity(DocumentoImportadoResponse.de(documento))
                .build();
    }

    @GET
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DocumentoImportadoResponse> listar(@QueryParam("status") StatusDocumento status) {
        return listarUseCase.executar(usuarioIdAutenticado(), status).stream()
                .map(DocumentoImportadoResponse::de)
                .toList();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("usuario")
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentoImportadoResponse buscarPorId(@PathParam("id") UUID id) {
        return DocumentoImportadoResponse.de(buscarUseCase.executar(id, usuarioIdAutenticado()));
    }

    @POST
    @Path("/{id}/confirmar")
    @RolesAllowed("usuario")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response confirmar(@PathParam("id") UUID id, @Valid ConfirmarLancamentosRequest request) {
        confirmarUseCase.executar(new ConfirmarLancamentosCommand(
                id, usuarioIdAutenticado(), request.contaId(), request.lancamentoIdsConfirmados()));
        return Response.noContent().build();
    }

    /** sub do token OIDC = id do usuário no Keycloak — nunca aceitar usuarioId vindo do cliente (ADR-0003). */
    private UUID usuarioIdAutenticado() {
        if (identity.getPrincipal() instanceof JsonWebToken jwt) {
            return UUID.fromString(jwt.getSubject());
        }
        throw new IllegalStateException("Token autenticado não é um JWT com claim 'sub'");
    }
}
