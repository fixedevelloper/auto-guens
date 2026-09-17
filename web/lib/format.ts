export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("fr-FR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatAmount(amount: string, countryCode: string): string {
  const currency = countryCode === "CM" ? "XAF" : countryCode;
  return `${Number(amount).toLocaleString("fr-FR")} ${currency}`;
}

const STATUS_STYLES: Record<string, string> = {
  PENDING: "bg-slate-100 text-slate-700",
  SENT_TO_DEVICE: "bg-blue-100 text-blue-700",
  RECEIVED_BY_DEVICE: "bg-blue-100 text-blue-700",
  EXECUTING: "bg-amber-100 text-amber-700",
  SUCCESS: "bg-green-100 text-green-700",
  FAILED: "bg-red-100 text-red-700",
  TIMEOUT: "bg-red-100 text-red-700",
  ONLINE: "bg-green-100 text-green-700",
  BUSY: "bg-amber-100 text-amber-700",
  OFFLINE: "bg-slate-100 text-slate-500",
};

export function statusBadgeClass(status: string): string {
  return STATUS_STYLES[status] ?? "bg-slate-100 text-slate-700";
}
