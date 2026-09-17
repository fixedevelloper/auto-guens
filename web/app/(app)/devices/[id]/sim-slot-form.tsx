"use client";

import { useActionState, useEffect, useRef } from "react";
import { upsertSimSlotAction, type SimSlotFormState } from "@/lib/device-actions";

const initialState: SimSlotFormState = {};

export default function SimSlotForm({ deviceId }: { deviceId: string }) {
  const action = upsertSimSlotAction.bind(null, deviceId);
  const [state, formAction, pending] = useActionState(action, initialState);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.success) formRef.current?.reset();
  }, [state.success]);

  return (
    <form ref={formRef} action={formAction} className="flex flex-wrap items-end gap-3 text-sm">
      <label className="flex flex-col gap-1">
        <span className="text-xs font-medium text-slate-500">Slot</span>
        <input
          type="number"
          name="slotIndex"
          min={0}
          required
          className="w-20 rounded-md border border-slate-300 px-2 py-1.5"
        />
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-xs font-medium text-slate-500">Opérateur</span>
        <select name="operator" required className="rounded-md border border-slate-300 px-2 py-1.5">
          <option value="MTN">MTN</option>
          <option value="ORANGE">ORANGE</option>
        </select>
      </label>
      <label className="flex flex-col gap-1">
        <span className="text-xs font-medium text-slate-500">Numéro sur la puce</span>
        <input
          type="text"
          name="phoneNumberOnSim"
          placeholder="677000000"
          className="rounded-md border border-slate-300 px-2 py-1.5"
        />
      </label>
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-slate-900 px-3 py-1.5 font-medium text-white hover:bg-slate-800 disabled:opacity-50"
      >
        {pending ? "Enregistrement..." : "Ajouter / mettre à jour"}
      </button>
      {state.error && <p className="w-full text-red-600">{state.error}</p>}
    </form>
  );
}
