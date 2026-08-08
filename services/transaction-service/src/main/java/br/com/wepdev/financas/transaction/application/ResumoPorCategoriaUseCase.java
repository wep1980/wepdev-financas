package br.com.wepdev.financas.transaction.application;

import br.com.wepdev.financas.transaction.domain.IntervaloInvalidoException;
import br.com.wepdev.financas.transaction.domain.Transacao;
import br.com.wepdev.financas.transaction.domain.TransacaoFiltro;
import br.com.wepdev.financas.transaction.domain.TransacaoRepository;
import br.com.wepdev.financas.transaction.domain.TipoTransacao;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Agrega DESPESAS confirmadas por categoria num período — mesmo cálculo
 * consumido pelo dashboard (PRD 3.7) e pela tool de IA
 * resumo_gastos_por_categoria (um endpoint, dois consumidores).
 */
@ApplicationScoped
public class ResumoPorCategoriaUseCase {

    private static final String SEM_CATEGORIA = "Sem categoria";

    private final TransacaoRepository transacaoRepository;

    public ResumoPorCategoriaUseCase(TransacaoRepository transacaoRepository) {
        this.transacaoRepository = transacaoRepository;
    }

    public List<ResumoCategoria> executar(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IntervaloInvalidoException(inicio, fim);
        }

        Map<String, BigDecimal> totalPorCategoria = totalDespesasPorCategoria(usuarioId, inicio, fim);

        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        LocalDate fimAnterior = inicio.minusDays(1);
        LocalDate inicioAnterior = fimAnterior.minusDays(dias - 1);
        Map<String, BigDecimal> totalPeriodoAnterior = totalDespesasPorCategoria(usuarioId, inicioAnterior, fimAnterior);

        BigDecimal totalGeral = totalPorCategoria.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalPorCategoria.entrySet().stream()
                .map(entry -> new ResumoCategoria(
                        entry.getKey(),
                        entry.getValue(),
                        percentual(entry.getValue(), totalGeral),
                        totalPeriodoAnterior.get(entry.getKey())
                ))
                .sorted(Comparator.comparing(ResumoCategoria::totalGasto).reversed()
                        .thenComparing(ResumoCategoria::categoria))
                .toList();
    }

    private Map<String, BigDecimal> totalDespesasPorCategoria(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        return transacaoRepository.listar(new TransacaoFiltro(usuarioId, null, inicio, fim)).stream()
                .filter(t -> t.getTipo() == TipoTransacao.DESPESA && !t.isCancelada())
                .collect(Collectors.groupingBy(
                        this::categoriaOuSemCategoria,
                        Collectors.reducing(BigDecimal.ZERO, Transacao::getValor, BigDecimal::add)
                ));
    }

    private String categoriaOuSemCategoria(Transacao transacao) {
        return transacao.getCategoria() == null ? SEM_CATEGORIA : transacao.getCategoria();
    }

    private BigDecimal percentual(BigDecimal valor, BigDecimal totalGeral) {
        if (totalGeral.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return valor.multiply(BigDecimal.valueOf(100)).divide(totalGeral, 2, RoundingMode.HALF_UP);
    }
}
