package br.com.wepdev.financas.document.infrastructure.client;

import br.com.wepdev.financas.document.domain.CardServiceClient;
import br.com.wepdev.financas.document.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.CompraExistente;
import br.com.wepdev.financas.document.infrastructure.client.dto.LancarCompraRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CardServiceClientImpl implements CardServiceClient {

    @RestClient
    CardServiceUsuarioClient usuarioClient;

    @Override
    public List<CompraExistente> listarComprasAtivas(UUID cartaoId) {
        try {
            return usuarioClient.listarCompras(cartaoId).stream()
                    .map(dto -> dto.paraDominio())
                    .toList();
        } catch (WebApplicationException e) {
            throw traduzirErro(cartaoId, e);
        }
    }

    @Override
    public void lancarCompra(UUID cartaoId, String descricao, BigDecimal valorTotal, String categoria,
                              LocalDate dataCompra, int quantidadeParcelas) {
        try {
            usuarioClient.lancarCompra(cartaoId,
                    new LancarCompraRequestDto(descricao, valorTotal, categoria, dataCompra, quantidadeParcelas));
        } catch (WebApplicationException e) {
            throw traduzirErro(cartaoId, e);
        }
    }

    /** Reusa o 404 do card-service (cartão inexistente OU de outro usuário) como gate de autorização. */
    private RuntimeException traduzirErro(UUID cartaoId, WebApplicationException e) {
        if (e.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return new CartaoNaoEncontradoException(cartaoId);
        }
        return e;
    }
}
