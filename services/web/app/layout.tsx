import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

// Inter — decisão de refinamento visual (2026-08-10,
// docs/architecture/design-system.md): legibilidade, números fáceis de
// ler (tabular-nums funciona bem nela), pesos 400-700.
const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Finanças",
  description: "Sistema de finanças pessoais",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="pt-BR" className={`${inter.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
