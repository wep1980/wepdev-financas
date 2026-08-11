package br.com.wepdev.financas.document.infrastructure.rest.dto;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.ws.rs.core.MediaType;

public class UploadDocumentoForm {

    @RestForm("arquivo")
    public FileUpload arquivo;

    @RestForm("tipo")
    @PartType(MediaType.TEXT_PLAIN)
    public String tipo;

    /** Cartão (card-service) que essa fatura pertence — obrigatório (ADR-0028). */
    @RestForm("cartaoId")
    @PartType(MediaType.TEXT_PLAIN)
    public String cartaoId;

    /** Nulo = PDF sem proteção. Nunca logada. */
    @RestForm("senha")
    @PartType(MediaType.TEXT_PLAIN)
    public String senha;
}
