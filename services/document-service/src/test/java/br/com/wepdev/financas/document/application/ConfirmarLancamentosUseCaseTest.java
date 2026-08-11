package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.CardServiceClient;
import br.com.wepdev.financas.document.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.CompraExistente;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.NenhumLancamentoSelecionadoException;
import br.com.wepdev.financas.document.domain.StatusDocumento;
import br.com.wepdev.financas.document.domain.StatusLancamento;
import br.com.wepdev.financas.document.domain.TipoDocumento;
import br.com.wepdev.financas.document.domain.TipoLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmarLancamentosUseCaseTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final CardServiceClient cardServiceClient = mock(CardServiceClient.class);
    private final ConfirmarLancamentosUseCase useCase = new ConfirmarLancamentosUseCase(repository, cardServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID cartaoId = UUID.randomUUID();

    private DocumentoImportado documentoAguardandoConfirmacao(LancamentoPendente... lancamentos) {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, cartaoId,
                "a.pdf", "x".getBytes());
        documento.concluirComLancamentos(List.of(lancamentos));
        return documento;
    }

    @Test
    void deveriaLancarCompraAVista_noCardService() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Alimentação", 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(List.of());

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId())));

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.CONFIRMADO);
        verify(repository).salvar(documento);
        verify(cardServiceClient).lancarCompra(cartaoId, "Mercado", new BigDecimal("50.00"), "Alimentação",
                LocalDate.of(2026, 8, 5), 1);
    }

    @Test
    void deveriaLancarCompraParcelada_desdeAPrimeiraParcela() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Notebook - Parcela 1/3",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Eletrônicos", 1, 3);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(List.of());

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId())));

        verify(cardServiceClient).lancarCompra(cartaoId, "Notebook", new BigDecimal("300.00"), "Eletrônicos",
                LocalDate.of(2026, 8, 5), 3);
    }

    @Test
    void deveriaLancarSoAsParcelasRestantes_quandoUploadPegaCompraNoMeioDaSequencia() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Notebook - Parcela 8/11",
                new BigDecimal("100.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, "Eletrônicos", 8, 11);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(List.of());

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId())));

        // 11 - 8 + 1 = 4 parcelas restantes, valor total = 100 * 4 = 400
        verify(cardServiceClient).lancarCompra(cartaoId, "Notebook", new BigDecimal("400.00"), "Eletrônicos",
                LocalDate.of(2026, 8, 5), 4);
    }

    @Test
    void naoDeveriaLancarCompraDeNovo_quandoJaExisteComMesmaAssinatura() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Notebook - Parcela 9/11",
                new BigDecimal("100.00"), LocalDate.of(2026, 9, 5), TipoLancamento.DESPESA, "Eletrônicos", 9, 11);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(
                List.of(new CompraExistente("Notebook", new BigDecimal("100.00"))));

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId())));

        verify(cardServiceClient, never()).lancarCompra(any(), any(), any(), any(), any(), any(Integer.class));
    }

    @Test
    void naoDeveriaLancarNoCardService_lancamentoDoTipoReceita() {
        LancamentoPendente estorno = LancamentoPendente.extrair(UUID.randomUUID(), "Estorno loja",
                new BigDecimal("30.00"), LocalDate.of(2026, 8, 6), TipoLancamento.RECEITA, null, 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(estorno);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(List.of());

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(estorno.getId())));

        verify(cardServiceClient, never()).lancarCompra(any(), any(), any(), any(), any(), any(Integer.class));
    }

    @Test
    void deveriaLancarSoOsConfirmados_ignorandoOsRejeitados() {
        LancamentoPendente confirmado = LancamentoPendente.extrair(UUID.randomUUID(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1);
        LancamentoPendente rejeitado = LancamentoPendente.extrair(UUID.randomUUID(), "Não é meu",
                new BigDecimal("999.00"), LocalDate.of(2026, 8, 6), TipoLancamento.DESPESA, null, 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(confirmado, rejeitado);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenReturn(List.of());

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(confirmado.getId())));

        assertThat(rejeitado.getStatus()).isEqualTo(StatusLancamento.REJEITADO);
        verify(cardServiceClient, times(1)).lancarCompra(eq(cartaoId), eq("Mercado"), any(), any(), any(), eq(1));
    }

    @Test
    void deveriaSerIdempotente_eNaoChamarCardServiceDeNovo_quandoJaConfirmado() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        documento.confirmar(Set.of(lancamento.getId()));
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId())));

        verify(repository, never()).salvar(any());
        verify(cardServiceClient, never()).listarComprasAtivas(any());
    }

    @Test
    void deveriaLancarExcecao_quandoNenhumLancamentoSelecionado() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of())))
                .isInstanceOf(NenhumLancamentoSelecionadoException.class);

        verify(cardServiceClient, never()).listarComprasAtivas(any());
    }

    @Test
    void deveriaLancarExcecao_quandoDocumentoNaoEncontrado() {
        UUID documentoId = UUID.randomUUID();
        when(repository.buscarPorId(documentoId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documentoId, usuarioId, Set.of(UUID.randomUUID()))))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
    }

    @Test
    void deveriaPropagarExcecao_eNaoSalvar_quandoCartaoNaoPertenceAoUsuario() {
        LancamentoPendente lancamento = LancamentoPendente.extrair(UUID.randomUUID(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null, 1, 1);
        DocumentoImportado documento = documentoAguardandoConfirmacao(lancamento);
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        when(cardServiceClient.listarComprasAtivas(cartaoId)).thenThrow(new CartaoNaoEncontradoException(cartaoId));

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documento.getId(), usuarioId, Set.of(lancamento.getId()))))
                .isInstanceOf(CartaoNaoEncontradoException.class);

        verify(repository, never()).salvar(any());
    }
}
