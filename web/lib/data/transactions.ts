import "server-only";
import { apiGet } from "@/lib/api";
import type { TransactionDetail, TransactionFilters, TransactionSummary } from "@/lib/types";

export function listTransactions(filters: TransactionFilters): Promise<TransactionSummary[]> {
  const params = new URLSearchParams();
  if (filters.operationType) params.set("operationType", filters.operationType);
  if (filters.operator) params.set("operator", filters.operator);
  if (filters.statut) params.set("statut", filters.statut);
  if (filters.from) params.set("from", filters.from);
  if (filters.to) params.set("to", filters.to);
  const qs = params.toString();
  return apiGet<TransactionSummary[]>(`/api/transactions${qs ? `?${qs}` : ""}`);
}

export function getTransaction(id: string): Promise<TransactionDetail> {
  return apiGet<TransactionDetail>(`/api/transactions/${id}`);
}
