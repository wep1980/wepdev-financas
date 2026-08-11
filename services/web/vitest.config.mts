import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  resolve: {
    tsconfigPaths: true,
    // "server-only" (usado em lib/*.ts que só devem rodar no servidor)
    // depende do bundler do Next.js pra virar no-op — fora dele (aqui,
    // Vitest puro) ele lança erro de propósito. Sem efeito no build/dev
    // real, só no ambiente de teste.
    alias: {
      "server-only": fileURLToPath(
        new URL("./vitest.server-only-stub.ts", import.meta.url)
      ),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
  },
});
