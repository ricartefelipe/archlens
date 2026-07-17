import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "ArchLens",
    template: "%s | ArchLens",
  },
  description:
    "Diagnóstico arquitetural e modernização de aplicações Java legadas — análise local, sem envio de código-fonte.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR">
      <body className="antialiased">{children}</body>
    </html>
  );
}
