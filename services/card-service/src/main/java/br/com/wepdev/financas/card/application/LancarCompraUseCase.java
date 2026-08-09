package br.com.wepdev.financas.card.application;

import br.com.wepdev.financas.card.domain.Cartao;
import br.com.wepdev.financas.card.domain.CartaoNaoEncontradoException;
import br.com.wepdev.financas.card.domain.CartaoRepository;
import br.com.wepdev.financas.card.domain.Fatura;
import br.com.wepdev.financas.card.domain.FaturaRepository;
import br.com.wepdev.financas.card.domain.Parcela;
import br.com.wepdev.financas.card.domain.ParcelaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Distribui uma compra (à vista ou parcelada) nas faturas correspondentes,
 * criando cada fatura automaticamente se ainda não existir — não há
 * endpoint de criação manual de fatura (docs/specs/card-service.yaml).
 * Não chama o account-service aqui; só ao pagar a fatura.
 */
@ApplicationScoped
public class LancarCompraUseCase {

    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;
    private final ParcelaRepository parcelaRepository;

    public LancarCompraUseCase(CartaoRepository cartaoRepository, FaturaRepository faturaRepository,
                                ParcelaRepository parcelaRepository) {
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.parcelaRepository = parcelaRepository;
    }

    @Transactional
    public CompraResultado executar(LancarCompraCommand command) {
        Cartao cartao = cartaoRepository.buscarPorId(command.cartaoId())
                .filter(c -> c.getUsuarioId().equals(command.usuarioId()))
                .orElseThrow(() -> new CartaoNaoEncontradoException(command.cartaoId()));

        UUID compraId = UUID.randomUUID();
        List<BigDecimal> valoresParcelas = dividir(command.valorTotal(), command.quantidadeParcelas());
        YearMonth competenciaInicial = competenciaDaPrimeiraParcela(command.dataCompra(), cartao.getDiaFechamento());

        List<ParcelaGerada> parcelasGeradas = new ArrayList<>();
        for (int i = 0; i < command.quantidadeParcelas(); i++) {
            YearMonth competencia = competenciaInicial.plusMonths(i);
            Fatura fatura = buscarOuCriarFatura(cartao, competencia);
            BigDecimal valorParcela = valoresParcelas.get(i);

            Parcela parcela = Parcela.criar(fatura.getId(), compraId, command.descricao(), valorParcela,
                    command.categoria(), i + 1, command.quantidadeParcelas());
            fatura.adicionarParcela(valorParcela);

            parcelaRepository.salvar(parcela);
            faturaRepository.salvar(fatura);
            parcelasGeradas.add(new ParcelaGerada(fatura.getId(), competencia, i + 1, valorParcela));
        }

        return new CompraResultado(compraId, cartao.getId(), command.descricao(), command.valorTotal(),
                command.categoria(), command.dataCompra(), command.quantidadeParcelas(), parcelasGeradas);
    }

    private Fatura buscarOuCriarFatura(Cartao cartao, YearMonth competencia) {
        return faturaRepository.buscarPorCartaoECompetencia(cartao.getId(), competencia)
                .orElseGet(() -> {
                    LocalDate dataFechamento = diaDoMesClamped(competencia, cartao.getDiaFechamento());
                    LocalDate dataVencimento = diaDoMesClamped(competencia, cartao.getDiaVencimento());
                    Fatura nova = Fatura.criar(cartao.getId(), cartao.getUsuarioId(), competencia, dataFechamento,
                            dataVencimento);
                    faturaRepository.salvar(nova);
                    return nova;
                });
    }

    /** diaFechamento/diaVencimento podem ser 29-31 — em meses menores, cai no último dia do mês. */
    private LocalDate diaDoMesClamped(YearMonth competencia, int dia) {
        return competencia.atDay(Math.min(dia, competencia.lengthOfMonth()));
    }

    /** Antes do fechamento do mês da compra → cai na fatura desse mês; senão, na do mês seguinte. */
    private YearMonth competenciaDaPrimeiraParcela(LocalDate dataCompra, int diaFechamento) {
        YearMonth mesDaCompra = YearMonth.from(dataCompra);
        if (dataCompra.getDayOfMonth() < diaFechamento) {
            return mesDaCompra;
        }
        return mesDaCompra.plusMonths(1);
    }

    /** Divide o valor total em N parcelas iguais (2 casas, HALF_UP) — a diferença de arredondamento cai na última. */
    private List<BigDecimal> dividir(BigDecimal valorTotal, int quantidadeParcelas) {
        BigDecimal valorBase = valorTotal.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.HALF_UP);
        List<BigDecimal> valores = new ArrayList<>();
        BigDecimal somaParcial = BigDecimal.ZERO;
        for (int i = 0; i < quantidadeParcelas - 1; i++) {
            valores.add(valorBase);
            somaParcial = somaParcial.add(valorBase);
        }
        valores.add(valorTotal.subtract(somaParcial));
        return valores;
    }
}
