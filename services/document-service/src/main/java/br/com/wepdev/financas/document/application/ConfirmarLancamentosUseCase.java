package br.com.wepdev.financas.document.application;

import br.com.wepdev.financas.document.domain.CardServiceClient;
import br.com.wepdev.financas.document.domain.CompraExistente;
import br.com.wepdev.financas.document.domain.DocumentoImportado;
import br.com.wepdev.financas.document.domain.DocumentoNaoEncontradoException;
import br.com.wepdev.financas.document.domain.DocumentoRepository;
import br.com.wepdev.financas.document.domain.LancamentoPendente;
import br.com.wepdev.financas.document.domain.NenhumLancamentoSelecionadoException;
import br.com.wepdev.financas.document.domain.TipoLancamento;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Idempotente: confirmar de novo um documento já CONFIRMADO não faz nada
 * (a checagem de status acontece antes de tocar em qualquer coisa).
 *
 * <p>Cada lançamento confirmado vira uma compra nova no {@code card-service}
 * (à vista ou parcelada, ADR-0028, 2026-08-11) — exceto os já conhecidos de
 * um upload anterior (dedup por assinatura, ver {@link #jaExiste}) e os do
 * tipo RECEITA (estorno/crédito): o modelo de {@code Parcela} do
 * card-service não tem conceito de valor negativo/estorno ainda —
 * limitação conhecida, aceita pro MVP (revisitar se virar problema real).
 *
 * <p>Lança as compras ANTES de persistir a confirmação de propósito: se
 * uma chamada ao card-service falhar no meio de várias, o documento
 * continua AGUARDANDO_CONFIRMACAO — reexecutar a confirmação é seguro
 * porque o dedup consulta o card-service de novo e pula o que já foi
 * lançado com sucesso na tentativa anterior.
 */
@ApplicationScoped
public class ConfirmarLancamentosUseCase {

    private final DocumentoRepository repository;
    private final CardServiceClient cardServiceClient;

    public ConfirmarLancamentosUseCase(DocumentoRepository repository, CardServiceClient cardServiceClient) {
        this.repository = repository;
        this.cardServiceClient = cardServiceClient;
    }

    public void executar(ConfirmarLancamentosCommand command) {
        DocumentoImportado documento = repository.buscarPorId(command.documentoId(), command.usuarioId())
                .orElseThrow(() -> new DocumentoNaoEncontradoException(command.documentoId()));

        if (documento.isConfirmado()) {
            return;
        }
        if (command.lancamentoIdsConfirmados().isEmpty()) {
            throw new NenhumLancamentoSelecionadoException();
        }

        documento.confirmar(command.lancamentoIdsConfirmados());
        lancarComprasNovas(documento);
        repository.salvar(documento);
    }

    private void lancarComprasNovas(DocumentoImportado documento) {
        List<CompraExistente> existentes = cardServiceClient.listarComprasAtivas(documento.getCartaoId());

        for (LancamentoPendente lancamento : documento.getLancamentosConfirmados()) {
            if (lancamento.getTipo() != TipoLancamento.DESPESA) {
                continue;
            }
            if (jaExiste(lancamento, existentes)) {
                continue;
            }
            lancarCompra(documento.getCartaoId(), lancamento);
        }
    }

    /**
     * Mesma descrição-base + valor de parcela, ainda ativa no cartão.
     * NÃO compara quantidadeParcelas de propósito (achado real testando
     * na prática, 2026-08-11): quando uma compra é registrada no meio da
     * sequência (ver {@link #lancarCompra}), o card-service passa a
     * guardar só as parcelas RESTANTES (ex: 5), enquanto o PDF de um
     * próximo mês continua mostrando o total ORIGINAL fixo (ex: "9/12",
     * sempre 12) — comparar quantidadeParcelas quebraria o dedup
     * exatamente no caso mais comum (fatura reaparecendo mês a mês).
     */
    private boolean jaExiste(LancamentoPendente lancamento, List<CompraExistente> existentes) {
        return existentes.stream().anyMatch(compra ->
                compra.descricao().equalsIgnoreCase(lancamento.getDescricaoBase())
                        && compra.valorParcela().compareTo(lancamento.getValor()) == 0);
    }

    /**
     * Se a fatura pegou a compra no meio da sequência (ex: "Parcela 8/11"
     * — a compra começou antes do usuário subir a primeira fatura no
     * sistema), registra só as parcelas restantes: o card-service não tem
     * como saber que já existiam 7 parcelas anteriores. A numeração
     * exibida reinicia (viraria "1/4" em vez de "9/11" em diante) —
     * cosmético, não afeta valor nem data (ver ADR-0028).
     */
    private void lancarCompra(UUID cartaoId, LancamentoPendente lancamento) {
        int parcelasRestantes = lancamento.getQuantidadeParcelas() - lancamento.getNumeroParcela() + 1;
        BigDecimal valorTotalRestante = lancamento.getValor().multiply(BigDecimal.valueOf(parcelasRestantes));
        cardServiceClient.lancarCompra(cartaoId, lancamento.getDescricaoBase(), valorTotalRestante,
                lancamento.getCategoriaSugerida(), lancamento.getData(), parcelasRestantes);
    }
}
