"use client";

import { Button } from "@/components/ui/button";

interface Props {
  action: (formData: FormData) => void | Promise<void>;
  hiddenFields: Record<string, string>;
  confirmMessage: string;
  children: React.ReactNode;
  variant?: React.ComponentProps<typeof Button>["variant"];
  size?: React.ComponentProps<typeof Button>["size"];
}

/** Form com confirm() nativo antes de submeter — usado em toda ação
 * destrutiva/irreversível (excluir conta, cancelar transação, cancelar
 * regra recorrente, ...). confirm() só existe em Client Component. */
export function ConfirmActionButton({
  action,
  hiddenFields,
  confirmMessage,
  children,
  variant = "ghost",
  size = "sm",
}: Props) {
  return (
    <form
      action={action}
      onSubmit={(evento) => {
        if (!confirm(confirmMessage)) {
          evento.preventDefault();
        }
      }}
    >
      {Object.entries(hiddenFields).map(([name, value]) => (
        <input key={name} type="hidden" name={name} value={value} />
      ))}
      <Button type="submit" variant={variant} size={size}>
        {children}
      </Button>
    </form>
  );
}
