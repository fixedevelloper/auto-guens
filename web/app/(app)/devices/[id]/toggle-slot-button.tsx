"use client";

import { useTransition } from "react";
import { setSimSlotActiveAction } from "@/lib/device-actions";

export default function ToggleSlotButton({
  deviceId,
  slotIndex,
  actif,
}: {
  deviceId: string;
  slotIndex: number;
  actif: boolean;
}) {
  const [pending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={pending}
      onClick={() => startTransition(() => setSimSlotActiveAction(deviceId, slotIndex, !actif))}
      className="text-xs text-slate-500 hover:text-slate-900 disabled:opacity-50"
    >
      {actif ? "Désactiver" : "Réactiver"}
    </button>
  );
}
