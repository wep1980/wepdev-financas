export interface NavItem {
  href: string;
  label: string;
  implementado: boolean;
}

/** implementado:false ainda aparece no menu (mapa do que vem por aí),
 * mas sem link — evita 404 clicando em algo que a fatia 6 ainda não
 * construiu (ver docs/tasks.md itens 4-8). */
export const NAV_ITEMS: NavItem[] = [
  { href: "/", label: "Dashboard", implementado: true },
  { href: "/contas", label: "Contas", implementado: true },
  { href: "/cartoes", label: "Cartões", implementado: true },
  { href: "/transacoes", label: "Transações", implementado: true },
  { href: "/documentos", label: "Documentos", implementado: true },
  { href: "/chat", label: "Chat IA", implementado: true },
];
