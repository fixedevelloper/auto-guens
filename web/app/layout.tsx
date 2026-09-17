import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "USSD Automation — Admin",
  description: "Configuration des devices/SIM et suivi des transactions",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body className="min-h-screen bg-slate-50 text-slate-900 antialiased">{children}</body>
    </html>
  );
}
