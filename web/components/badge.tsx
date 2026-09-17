import { statusBadgeClass } from "@/lib/format";

export function Badge({ status }: { status: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusBadgeClass(status)}`}
    >
      {status}
    </span>
  );
}
