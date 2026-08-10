package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.AccountServiceClient;
import br.com.wepdev.financas.document.domain.ContaNaoEncontradaException;
import br.com.wepdev.financas.document.domain.DocumentoEventPublisher;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmarLancamentosUseCaseTest {

    private final DocumentoRepository repository = mock(DocumentoRepository.class);
    private final DocumentoEventPublisher eventPublisher = mock(DocumentoEventPublisher.class);
    private final AccountServiceClient accountServiceClient = mock(AccountServiceClient.class);
    private final ConfirmarLancamentosUseCase useCase =
            new ConfirmarLancamentosUseCase(repository, eventPublisher, accountServiceClient);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID contaId = UUID.randomUUID();

    private DocumentoImportado documentoAguardandoConfirmacao() {
        DocumentoImportado documento = DocumentoImportado.receber(usuarioId, TipoDocumento.FATURA_CARTAO, "a.pdf", "x".getBytes());
        LancamentoPendente lancamento = LancamentoPendente.extrair(documento.getId(), "Mercado",
                new BigDecimal("50.00"), LocalDate.of(2026, 8, 5), TipoLancamento.DESPESA, null);
        documento.concluirComLancamentos(List.of(lancamento));
        return documento;
    }

    @Test
    void deveriaConfirmarLancamentosSelecionados_ePublicarEvento() {
        DocumentoImportado documento = documentoAguardandoConfirmacao();
        UUID lancamentoId = documento.getLancamentos().get(0).getId();
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, contaId, Set.of(lancamentoId)));

        assertThat(documento.getStatus()).isEqualTo(StatusDocumento.CONFIRMADO);
        assertThat(documento.getLancamentos().get(0).getStatus()).isEqualTo(StatusLancamento.CONFIRMADO);
        verify(repository).salvar(documento);
        verify(eventPublisher).publicarLancamentosConfirmados(documento, contaId);
    }

    @Test
    void deveriaSerIdempotente_eNaoRepublicarEvento_quandoJaConfirmado() {
        DocumentoImportado documento = documentoAguardandoConfirmacao();
        UUID lancamentoId = documento.getLancamentos().get(0).getId();
        documento.confirmar(Set.of(lancamentoId));
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, contaId, Set.of(lancamentoId)));

        verify(repository, never()).salvar(any());
        verify(eventPublisher, never()).publicarLancamentosConfirmados(any(), any());
    }

    @Test
    void deveriaLancarExcecao_quandoNenhumLancamentoSelecionado() {
        DocumentoImportado documento = documentoAguardandoConfirmacao();
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documento.getId(), usuarioId, contaId, Set.of())))
                .isInstanceOf(NenhumLancamentoSelecionadoException.class);

        verify(eventPublisher, never()).publicarLancamentosConfirmados(any(), any());
    }

    @Test
    void deveriaLancarExcecao_quandoDocumentoNaoEncontrado() {
        UUID documentoId = UUID.randomUUID();
        when(repository.buscarPorId(documentoId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documentoId, usuarioId, contaId, Set.of(UUID.randomUUID()))))
                .isInstanceOf(DocumentoNaoEncontradoException.class);
    }

    @Test
    void deveriaPropagarExcecao_eNaoPublicarEvento_quandoContaNaoPertenceAoUsuario() {
        DocumentoImportado documento = documentoAguardandoConfirmacao();
        UUID lancamentoId = documento.getLancamentos().get(0).getId();
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));
        doThrow(new ContaNaoEncontradaException(contaId)).when(accountServiceClient).confirmarPosseDaConta(contaId);

        assertThatThrownBy(() -> useCase.executar(
                new ConfirmarLancamentosCommand(documento.getId(), usuarioId, contaId, Set.of(lancamentoId))))
                .isInstanceOf(ContaNaoEncontradaException.class);

        assertThat(documento.getStatus()).isNotEqualTo(StatusDocumento.CONFIRMADO);
        verify(repository, never()).salvar(any());
        verify(eventPublisher, never()).publicarLancamentosConfirmados(any(), any());
    }

    @Test
    void deveriaPublicarSoOsLancamentosConfirmados_quandoConfirmacaoParcial() {
        DocumentoImportado documento = documentoAguardandoConfirmacao();
        LancamentoPendente segundo = LancamentoPendente.extrair(documento.getId(), "Farmácia",
                new BigDecimal("20.00"), LocalDate.of(2026, 8, 6), TipoLancamento.DESPESA, null);
        documento.concluirComLancamentos(List.of(documento.getLancamentos().get(0), segundo));
        UUID idConfirmado = documento.getLancamentos().get(0).getId();
        when(repository.buscarPorId(documento.getId(), usuarioId)).thenReturn(Optional.of(documento));

        useCase.executar(new ConfirmarLancamentosCommand(documento.getId(), usuarioId, contaId, Set.of(idConfirmado)));

        assertThat(documento.getLancamentosConfirmados()).hasSize(1);
        assertThat(documento.getLancamentosConfirmados().get(0).getId()).isEqualTo(idConfirmado);
        verify(eventPublisher, times(1)).publicarLancamentosConfirmados(documento, contaId);
    }
}
