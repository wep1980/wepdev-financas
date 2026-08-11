import { cn } from "@/lib/utils";

/**
 * Indeterminada de propósito — não temos % real de progresso (extração de
 * fatura é uma chamada única e bloqueante ao LLM), então a barra só
 * comunica "ainda processando", nunca finge uma porcentagem que não existe
 * (2026-08-11, ver historico.md).
 */
export function Progress({ className }: { className?: string }) {
  return (
    <div
      role="progressbar"
      aria-label="Processando"
      className={cn("bg-muted relative h-2 w-full overflow-hidden rounded-full", className)}
    >
      <div
        className="bg-primary absolute inset-y-0 w-2/5 rounded-full"
        style={{ animation: "progress-indeterminate 1.3s ease-in-out infinite" }}
      />
    </div>
  );
}
