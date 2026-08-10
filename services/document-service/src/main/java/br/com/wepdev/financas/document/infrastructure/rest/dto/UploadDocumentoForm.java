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

    /** Nulo = fatura de uma pessoa só (sem filtro de seção) — ver AgenteExtracaoFaturaService. */
    @RestForm("nomeFiltro")
    @PartType(MediaType.TEXT_PLAIN)
    public String nomeFiltro;

    /** Nulo = PDF sem proteção. Nunca logada. */
    @RestForm("senha")
    @PartType(MediaType.TEXT_PLAIN)
    public String senha;
}
