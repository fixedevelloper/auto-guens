"use client";

import { useActionState, useState } from "react";
import Link from "next/link";
import { createDeviceAction, type CreateDeviceState } from "@/lib/device-actions";

const initialState: CreateDeviceState = {};

export default function CreateDeviceButton() {
  const [state, formAction, pending] = useActionState(createDeviceAction, initialState);
  const [copied, setCopied] = useState(false);

  if (state.device) {
    return (
      <div className="rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm">
        <p className="mb-2 font-medium text-amber-900">
          Device créé — notez la clé API maintenant, elle ne sera plus jamais affichée.
        </p>
        <dl className="space-y-1">
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-amber-700">deviceId</dt>
            <dd className="font-mono break-all">{state.device.id}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-amber-700">apiKey</dt>
            <dd className="font-mono break-all">{state.device.apiKey}</dd>
          </div>
        </dl>
        <div className="mt-3 flex gap-3">
          <button
            type="button"
            onClick={() => {
              navigator.clipboard.writeText(
                `deviceId: ${state.device!.id}\napiKey: ${state.device!.apiKey}`
              );
              setCopied(true);
            }}
            className="rounded-md bg-amber-900 px-3 py-1.5 text-xs font-medium text-white hover:bg-amber-800"
          >
            {copied ? "Copié" : "Copier"}
          </button>
          <Link href={`/devices/${state.device.id}`} className="px-3 py-1.5 text-xs text-amber-900 underline">
            Configurer ses SIM
          </Link>
        </div>
      </div>
    );
  }

  return (
    <form action={formAction}>
      {state.error && <p className="mb-2 text-sm text-red-600">{state.error}</p>}
      <button
        type="submit"
        disabled={pending}
        className="rounded-md bg-slate-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-slate-800 disabled:opacity-50"
      >
        {pending ? "Création..." : "+ Nouveau device"}
      </button>
    </form>
  );
}
