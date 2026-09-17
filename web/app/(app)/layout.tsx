import Link from "next/link";
import { logout } from "@/lib/auth-actions";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
          <nav className="flex items-center gap-6 text-sm font-medium text-slate-600">
            <span className="font-semibold text-slate-900">USSD Automation</span>
            <Link href="/transactions" className="hover:text-slate-900">
              Transactions
            </Link>
            <Link href="/devices" className="hover:text-slate-900">
              Devices &amp; SIM
            </Link>
          </nav>
          <form action={logout}>
            <button type="submit" className="text-sm text-slate-500 hover:text-slate-900">
              Déconnexion
            </button>
          </form>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-6">{children}</main>
    </div>
  );
}
