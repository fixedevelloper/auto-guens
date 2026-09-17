import Link from "next/link";
import { notFound } from "next/navigation";
import { getTransaction } from "@/lib/data/transactions";
import { formatAmount, formatDate } from "@/lib/format";
import { Badge } from "@/components/badge";
import { ApiError } from "@/lib/api";

export const dynamic = "force-dynamic";

export default async function TransactionDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;

  let tx;
  try {
    tx = await getTransaction(id);
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link href="/transactions" className="text-sm text-slate-500 hover:text-slate-900">
          ← Transactions
        </Link>
      </div>

      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900">
          {tx.operationType} — {formatAmount(tx.amount, tx.countryCode)}
        </h1>
        <Badge status={tx.statut} />
      </div>

      <dl className="grid grid-cols-2 gap-x-6 gap-y-3 rounded-lg border border-slate-200 bg-white p-4 text-sm sm:grid-cols-3">
        <Field label="ID" value={tx.id} mono />
        <Field label="Téléphone" value={tx.phone} />
        <Field label="Opérateur" value={tx.operator} />
        <Field label="Expéditeur" value={tx.senderName} />
        <Field label="Description" value={tx.description ?? "—"} />
        <Field label="Device" value={tx.deviceId ?? "—"} mono />
        <Field label="Slot SIM" value={tx.simSlotUsed?.toString() ?? "—"} />
        <Field label="Code USSD" value={tx.ussdCodeUsed ?? "—"} mono />
        <Field label="Référence opérateur" value={tx.operatorReference ?? "—"} />
        <Field label="Créée le" value={formatDate(tx.createdAt)} />
        <Field label="Mise à jour" value={formatDate(tx.updatedAt)} />
        <Field label="Expire le" value={formatDate(tx.expiresAt)} />
        {tx.failureReason && (
          <div className="col-span-full">
            <Field label="Raison d'échec" value={tx.failureReason} />
          </div>
        )}
      </dl>

      <div>
        <h2 className="mb-2 text-sm font-semibold text-slate-900">Historique</h2>
        <ol className="space-y-2">
          {tx.statusHistory.map((entry, index) => (
            <li
              key={`${entry.statut}-${entry.receivedAt}-${index}`}
              className="flex items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2 text-sm"
            >
              <Badge status={entry.statut} />
              <span className="text-slate-500">{formatDate(entry.receivedAt)}</span>
              {entry.failureReason && <span className="text-red-600">{entry.failureReason}</span>}
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}

function Field({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div>
      <dt className="text-xs uppercase text-slate-400">{label}</dt>
      <dd className={mono ? "font-mono text-xs text-slate-800 break-all" : "text-slate-800"}>{value}</dd>
    </div>
  );
}
