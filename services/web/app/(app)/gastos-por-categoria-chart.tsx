"use client";

import { Bar, BarChart, CartesianGrid, XAxis } from "recharts";
import {
  ChartContainer,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from "@/components/ui/chart";
import type { ResumoCategoria } from "@/lib/transaction-service";

const chartConfig = {
  totalGasto: { label: "Este mês", color: "var(--chart-1)" },
  totalGastoPeriodoAnterior: { label: "Mês anterior", color: "var(--chart-2)" },
} satisfies ChartConfig;

export function GastosPorCategoriaChart({ dados }: { dados: ResumoCategoria[] }) {
  if (dados.length === 0) {
    return (
      <p className="text-muted-foreground py-8 text-center text-sm">
        Nenhum gasto confirmado nesse mês ainda.
      </p>
    );
  }

  return (
    <ChartContainer config={chartConfig} className="max-h-72 w-full">
      <BarChart data={dados} accessibilityLayer>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey="categoria"
          tickLine={false}
          axisLine={false}
          tickMargin={8}
        />
        <ChartTooltip content={<ChartTooltipContent />} />
        <Bar dataKey="totalGasto" fill="var(--color-totalGasto)" radius={4} />
        <Bar
          dataKey="totalGastoPeriodoAnterior"
          fill="var(--color-totalGastoPeriodoAnterior)"
          radius={4}
        />
      </BarChart>
    </ChartContainer>
  );
}
