import type { Metadata } from "next";
import "./globals.css";

import { QueryProvider } from "@/components/query-provider";
import { ThemeProvider } from "@/components/theme-provider";

export const metadata: Metadata = {
  title: "RunbookOS — Human-Governed AI Incident Response",
  description:
    "Receive production incident signals, collect diagnostic evidence, generate AI-grounded analysis, recommend runbooks, and execute remediation with human approval.",
  icons: {
    icon: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-screen antialiased">
        <ThemeProvider>
          <QueryProvider>{children}</QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
