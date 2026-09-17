"use client";

import { useActionState, useEffect, useRef } from "react";
import { createUssdTemplateAction, type UssdTemplateFormState } from "@/lib/ussd-template-actions";

const initialState: UssdTemplateFormState = {};

export default function CreateTemplateForm() {
  const [state, formAction, pending] = useActionState(createUssdTemplateAction, initialState);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (state.success) formRef.current?.reset();
  }, [state.success]);

  return (
    <form ref={formRef} action={formAction} className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
      <h2 className="text-sm font-semibold text-slate-900">Nouveau template</h2>
      <div className="flex flex-wrap items-end gap-3 text-sm">
        <label className="flex flex-col gap-1">
          <span className="text-xs font-medium text-slate-500">Opérateur</span>
          <select name="operator" required className="rounded-md border border-slate-300 px-2 py-1.5">
            <option value="MTN">MTN</option>
            <option value="ORANGE">ORANGE</option>
          </select>
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-xs font-medium text-slate-500">Opération</span>
          <select name="operationType" required className="rounded-md border border-slate-300 px-2 py-1.5">
            <option value="DEPOSIT">DEPOSIT</option>
            <option value="WITHDRAW">WITHDRAW</option>
          </select>
        </label>
        <label className="flex flex-1 min-w-64 flex-col gap-1">
          <span className="text-xs font-medium text-slate-500">Template (placeholders {"{amount}"} / {"{phone}"})</span>
          <input
            type="text"
            name="template"
            required
            placeholder="*126*1*{amount}*{phone}#"
            className="rounded-md border border-slate-300 px-2 py-1.5 font-mono"
          />
        </label>
        <button
          type="submit"
          disabled={pending}
          className="rounded-md bg-slate-900 px-3 py-1.5 font-medium text-white hover:bg-slate-800 disabled:opacity-50"
        >
          {pending ? "Création..." : "Créer"}
        </button>
      </div>
      {state.error && <p className="text-sm text-red-600">{state.error}</p>}
      <p className="text-xs text-slate-400">
        Créer un template pour un couple opérateur/opération désactive automatiquement l&apos;ancien
        template actif de ce couple (historique conservé, consultable ci-dessous).
      </p>
    </form>
  );
}
