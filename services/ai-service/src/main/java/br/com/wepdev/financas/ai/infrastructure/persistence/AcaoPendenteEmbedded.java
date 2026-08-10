package br.com.wepdev.financas.ai.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Documento embutido dentro de ConversaEntity, nullable — ver AcaoPendente no domínio. */
public class AcaoPendenteEmbedded {

    public String tipo;

    public String descricao;

    public BigDecimal valor;

    public boolean recorrente;

    public String frequencia;

    public Integer quantidadeOcorrencias;

    public UUID contaId;

    public String categoria;

    public Instant criadaEm;

    public Instant expiraEm;
}
