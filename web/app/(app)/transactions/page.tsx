import Link from "next/link";
import { listTransactions } from "@/lib/data/transactions";
import { formatAmount, formatDate } from "@/lib/format";
import { Badge } from "@/components/badge";
import { ApiError } from "@/lib/api";
import type { OperationType, Operator, TransactionStatus, TransactionSummary } from "@/lib/types";

const OPERATION_TYPES: OperationType[] = ["DEPOSIT", "WITHDRAW"];
const OPERATORS: Operator[] = ["MTN", "ORANGE"];
const STATUSES: TransactionStatus[] = [
  "PENDING",
  "SENT_TO_DEVICE",
  "RECEIVED_BY_DEVICE",
  "EXECUTING",
  "SUCCESS",
  "FAILED",
  "TIMEOUT",
];

export const dynamic = "force-dynamic";

interface SearchParams {
  operationType?: string;
  operator?: string;
  statut?: string;
}

export default async function TransactionsPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const params = await searchParams;
  const filters = {
    operationType: params.operationType as OperationType | undefined,
    operator: params.operator as Operator | undefined,
    statut: params.statut as TransactionStatus | undefined,
  };

  let transactions: TransactionSummary[];
  let error: string | null = null;
  try {
    transactions = await listTransactions(filters);
  } catch (e) {
    transactions = [];
    error = e instanceof ApiError ? e.message : "Erreur de connexion à l'API";
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-slate-900">Transactions</h1>
      </div>

      <form className="flex flex-wrap gap-3 text-sm" method="get">
        <Select name="operationType" label="Type" value={params.operationType} options={OPERATION_TYPES} />
        <Select name="operator" label="Opérateur" value={params.operator} options={OPERATORS} />
        <Select name="statut" label="Statut" value={params.statut} options={STATUSES} />
        <div className="flex items-end">
          <button type="submit" className="rounded-md bg-slate-900 px-3 py-1.5 text-white hover:bg-slate-800">
            Filtrer
          </button>
        </div>
        {(params.operationType || params.operator || params.statut) && (
          <div className="flex items-end">
            <Link href="/transactions" className="px-3 py-1.5 text-slate-500 hover:text-slate-900">
              Réinitialiser
            </Link>
          </div>
        )}
      </form>

      {error && (
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
      )}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-2">Date</th>
              <th className="px-4 py-2">Type</th>
              <th className="px-4 py-2">Opérateur</th>
              <th className="px-4 py-2">Téléphone</th>
              <th className="px-4 py-2">Montant</th>
              <th className="px-4 py-2">Statut</th>
              <th className="px-4 py-2" />
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {transactions.map((tx) => (
              <tr key={tx.id} className="hover:bg-slate-50">
                <td className="px-4 py-2 text-slate-500">{formatDate(tx.createdAt)}</td>
                <td className="px-4 py-2">{tx.operationType}</td>
                <td className="px-4 py-2">{tx.operator}</td>
                <td className="px-4 py-2">{tx.phone}</td>
                <td className="px-4 py-2">{formatAmount(tx.amount, tx.countryCode)}</td>
                <td className="px-4 py-2">
                  <Badge status={tx.statut} />
                </td>
                <td className="px-4 py-2 text-right">
                  <Link href={`/transactions/${tx.id}`} className="text-slate-600 hover:text-slate-900">
                    Détail
                  </Link>
                </td>
              </tr>
            ))}
            {transactions.length === 0 && !error && (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                  Aucune transaction
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function Select({
  name,
  label,
  value,
  options,
}: {
  name: string;
  label: string;
  value: string | undefined;
  options: string[];
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-slate-500">{label}</span>
      <select
        name={name}
        defaultValue={value ?? ""}
        className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
      >
        <option value="">Tous</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}
