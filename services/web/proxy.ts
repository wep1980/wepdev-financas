// Next.js 16 renomeou middleware.ts pra proxy.ts (mesma função, nome
// diferente — ver node_modules/next/dist/docs/.../file-conventions/proxy.md).
export { auth as proxy } from "@/auth";

export const config = {
  // /api/auth/* precisa ficar de fora — são as próprias rotas do Auth.js
  // (signin/callback/signout), interceptar essas quebraria o login.
  matcher: ["/((?!api/auth|_next/static|_next/image|favicon.ico).*)"],
};
