import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // "standalone" gera um server.js autocontido em .next/standalone —
  // é o que o Dockerfile (multi-stage) copia pra imagem final, sem
  // precisar de node_modules completo no runtime (mesmo espírito dos
  // Dockerfile.jvm dos serviços Java: imagem final enxuta).
  output: "standalone",
  experimental: {
    serverActions: {
      // Default do Next.js é 1MB — pequeno demais pra fatura em PDF real
      // (item 7, upload pro document-service). 10mb casa com o limite
      // que o próprio document-service já aceita
      // (quarkus.http.limits.max-body-size=10M).
      bodySizeLimit: "10mb",
    },
  },
};

export default nextConfig;
